package org.mosaic.lifecycle.impl.dependency;

import com.google.common.collect.ComparisonChain;
import com.google.common.reflect.TypeToken;
import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.lifecycle.impl.ModuleImpl;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.pair.ImmutableComparablePair;
import org.mosaic.util.pair.Pair;
import org.mosaic.util.reflection.MethodHandle;
import org.mosaic.util.reflection.MethodParameter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.System.identityHashCode;
import static java.util.Arrays.asList;
import static org.mosaic.lifecycle.impl.util.FilterUtils.createFilter;
import static org.mosaic.lifecycle.impl.util.ServiceUtils.getId;
import static org.mosaic.lifecycle.impl.util.ServiceUtils.getRanking;

/**
 * @author arik
 */
public class ServiceRefsDependency extends AbstractBeanDependency implements ServiceTrackerCustomizer<Object, Object>
{
    private static final Logger LOG = LoggerFactory.getLogger( ServiceRefsDependency.class );

    private static final ServiceRefsDependency.ServicePairComparator SERVICE_PAIR_COMPARATOR = new ServicePairComparator();

    private static class ServiceListResolver implements MethodHandle.ParameterResolver
    {
        @Nullable
        @Override
        public Object resolve( @Nonnull MethodParameter parameter, @Nonnull MapEx<String, Object> resolveContext )
        {
            return resolveContext.get( "services" );
        }
    }

    private static class ServicePairComparator implements Comparator<Pair<ServiceReference<Object>, Object>>
    {
        @Override
        public int compare( @Nullable Pair<ServiceReference<Object>, Object> o1,
                            @Nullable Pair<ServiceReference<Object>, Object> o2 )
        {
            if( o1 == o2 )
            {
                return 0;
            }
            else if( o1 == null )
            {
                return -1;
            }
            else if( o2 == null )
            {
                return 1;
            }
            else
            {
                ServiceReference<Object> l1 = o1.getLeft();
                ServiceReference<Object> l2 = o2.getLeft();

                // if one of the references are null - it's easy
                if( l1 == l2 )
                {
                    return 0;
                }
                else if( l1 == null )
                {
                    return -1;
                }
                else if( l2 == null )
                {
                    return 1;
                }
                else
                {
                    ComparisonChain chain = ComparisonChain.start();

                    // this comparison returns lower-rankings first and higher-ranking later
                    // we reverse this here by switching between o1 and o2
                    chain = chain.compare( getRanking( l2 ), getRanking( l1 ) );

                    // this comparison returns older-services first and newer-services later
                    // we reverse this here by switching between o1 and o2
                    chain = chain.compare( getId( l2 ), getId( l1 ) );

                    // finally, an object identity comparison
                    chain.compare( identityHashCode( o1.getRight() ), identityHashCode( o2.getRight() ) );

                    // return result
                    return chain.result();
                }
            }
        }
    }

    @Nonnull
    private final ModuleImpl module;

    @Nullable
    private final Class<?> serviceType;

    @Nullable
    private final String filter;

    @Nonnull
    private final String beanName;

    @Nonnull
    private final MethodHandle methodHandle;

    @Nonnull
    private final MethodHandle.Invoker invoker;

    @Nonnull
    private final Services services = new Services();

    @Nonnull
    private final Map<String, Object> resolveContext = new HashMap<>();

    @Nullable
    private ServiceTracker<?, ?> tracker;

    public ServiceRefsDependency( @Nonnull ModuleImpl module,
                                  @Nullable String filterSpec,
                                  @Nonnull String beanName,
                                  @Nonnull MethodHandle methodHandle )
    {
        this.module = module;
        this.filter = filterSpec;
        this.beanName = beanName;
        this.methodHandle = methodHandle;
        this.serviceType = detectServiceType();
        this.resolveContext.put( "services", this.services );
        this.invoker = this.methodHandle.createInvoker( new ServiceListResolver() );
    }

    @Nonnull
    public String getBeanName()
    {
        return this.beanName;
    }

    @Override
    public String toString()
    {
        return String.format( "ServiceRefs[%s] for %s",
                              createFilter( this.serviceType, this.filter ),
                              this.methodHandle );
    }

    @Override
    public void start()
    {
        if( this.serviceType != null )
        {
            Filter filter = createFilter( this.serviceType, this.filter );
            this.tracker = new ServiceTracker<>( this.module.getBundleContext(), filter, this );
            this.tracker.open();
        }
    }

    @Override
    public boolean isSatisfied()
    {
        return this.tracker != null;
    }

    @Override
    public void stop()
    {
        if( this.tracker != null )
        {
            this.tracker.close();
            this.tracker = null;
        }
    }

    @Override
    public Object addingService( ServiceReference<Object> reference )
    {
        Object service = null;

        BundleContext bundleContext = this.module.getBundleContext();
        if( bundleContext != null )
        {
            service = bundleContext.getService( reference );
            if( service != null )
            {
                this.services._addService( reference, service );
            }
        }

        return service;
    }

    @Override
    public void modifiedService( ServiceReference<Object> reference, Object service )
    {
        // no-op
    }

    @Override
    public void removedService( ServiceReference<Object> reference, Object service )
    {
        this.services._removeService( reference, service );
    }

    public void beanCreated( @Nonnull Object bean )
    {
        if( this.tracker != null )
        {
            try
            {
                this.invoker.resolve( this.resolveContext ).invoke( bean );
            }
            catch( Exception e )
            {
                LOG.error( "Could not inject services list to method '{}' in bean '{}' of module '{}': {}", this.methodHandle, bean, this.module, e.getMessage(), e );
            }
        }
    }

    @Override
    public void beanInitialized( @Nonnull Object bean )
    {
        // no-op
    }

    @Nullable
    private Class<?> detectServiceType()
    {
        List<MethodParameter> parameters = this.methodHandle.getParameters();
        if( parameters.size() != 1 )
        {
            // illegal number of parameters
            return null;
        }

        MethodParameter parameter = parameters.get( 0 );
        if( parameter.isList() )
        {
            TypeToken<?> itemType = parameter.getCollectionItemType();
            if( itemType != null )
            {
                return itemType.getRawType();
            }
        }

        // not a list, or unknown item type
        return null;
    }

    private class Services implements List<Object>
    {
        private SortedSet<Pair<ServiceReference<Object>, Object>> services = new TreeSet<>();

        @Override
        public int size()
        {
            return this.services.size();
        }

        @Override
        public boolean isEmpty()
        {
            return this.services.isEmpty();
        }

        @Override
        public boolean contains( Object o )
        {
            return this.services.contains( o );
        }

        @Nonnull
        @Override
        public Iterator<Object> iterator()
        {
            final List<Pair<ServiceReference<Object>, Object>> copy = new LinkedList<>( this.services );
            return new Iterator<Object>()
            {
                private int index = 0;

                @Override
                public boolean hasNext()
                {
                    return this.index < copy.size();
                }

                @Override
                public Object next()
                {
                    if( this.index >= copy.size() )
                    {
                        throw new NoSuchElementException();
                    }
                    else
                    {
                        return copy.get( this.index++ ).getValue();
                    }
                }

                @Override
                public void remove()
                {
                    throw new UnsupportedOperationException( "Services list cannot be modified" );
                }
            };
        }

        @Nonnull
        @Override
        public Object[] toArray()
        {
            final List<Pair<ServiceReference<Object>, Object>> copy = new ArrayList<>( this.services );
            Object[] items = new Object[ copy.size() ];
            for( int i = 0; i < copy.size(); i++ )
            {
                Pair<ServiceReference<Object>, Object> entry = copy.get( i );
                items[ i ] = entry.getValue();
            }
            return items;
        }

        @Nonnull
        @SuppressWarnings("unchecked")
        @Override
        public <T> T[] toArray( @Nonnull T[] a )
        {
            final List<Pair<ServiceReference<Object>, Object>> copy = new ArrayList<>( this.services );
            Object[] items = a.length >= copy.size() ? a : new Object[ copy.size() ];
            for( int i = 0; i < copy.size(); i++ )
            {
                Pair<ServiceReference<Object>, Object> entry = copy.get( i );
                items[ i ] = entry.getValue();
            }
            return ( T[] ) items;
        }

        @Override
        public boolean add( Object o )
        {
            throw new UnsupportedOperationException( "Services list cannot be modified" );
        }

        @Override
        public boolean remove( Object o )
        {
            throw new UnsupportedOperationException( "Services list cannot be modified" );
        }

        @Override
        public boolean containsAll( @Nonnull Collection<?> c )
        {
            return this.services.containsAll( c );
        }

        @Override
        public boolean addAll( @Nonnull Collection<?> c )
        {
            throw new UnsupportedOperationException( "Services list cannot be modified" );
        }

        @Override
        public boolean addAll( int index, @Nonnull Collection<?> c )
        {
            throw new UnsupportedOperationException( "Services list cannot be modified" );
        }

        @Override
        public boolean removeAll( @Nonnull Collection<?> c )
        {
            throw new UnsupportedOperationException( "Services list cannot be modified" );
        }

        @Override
        public boolean retainAll( @Nonnull Collection<?> c )
        {
            throw new UnsupportedOperationException( "Services list cannot be modified" );
        }

        @Override
        public void clear()
        {
            throw new UnsupportedOperationException( "Services list cannot be modified" );
        }

        @Override
        public Object get( int index )
        {
            return toArray()[ index ];
        }

        @Override
        public Object set( int index, Object element )
        {
            throw new UnsupportedOperationException( "Services list cannot be modified" );
        }

        @Override
        public void add( int index, Object element )
        {
            throw new UnsupportedOperationException( "Services list cannot be modified" );
        }

        @Override
        public Object remove( int index )
        {
            throw new UnsupportedOperationException( "Services list cannot be modified" );
        }

        @Override
        public int indexOf( Object o )
        {
            return new LinkedList<Object>( this.services ).indexOf( o );
        }

        @Override
        public int lastIndexOf( Object o )
        {
            return new LinkedList<Object>( this.services ).lastIndexOf( o );
        }

        @Nonnull
        @Override
        public ListIterator<Object> listIterator()
        {
            return listIterator( 0 );
        }

        @Nonnull
        @Override
        public ListIterator<Object> listIterator( int index )
        {
            final int nextIndex = index;
            final List<Pair<ServiceReference<Object>, Object>> copy = new LinkedList<>( this.services );
            return new ListIterator<Object>()
            {
                private int index = nextIndex;

                @Override
                public boolean hasNext()
                {
                    return this.index < copy.size();
                }

                @Override
                public Object next()
                {
                    if( this.index >= copy.size() )
                    {
                        throw new NoSuchElementException();
                    }
                    else
                    {
                        return copy.get( this.index++ ).getValue();
                    }
                }

                @Override
                public void remove()
                {
                    throw new UnsupportedOperationException( "Services list cannot be modified" );
                }

                @Override
                public boolean hasPrevious()
                {
                    return this.index > 0;
                }

                @Override
                public Object previous()
                {
                    if( this.index <= 0 )
                    {
                        throw new NoSuchElementException();
                    }
                    else
                    {
                        return copy.get( --this.index ).getValue();
                    }
                }

                @Override
                public int nextIndex()
                {
                    return this.index;
                }

                @Override
                public int previousIndex()
                {
                    return this.index - 1;
                }

                @Override
                public void set( Object o )
                {
                    throw new UnsupportedOperationException( "Services list cannot be modified" );
                }

                @Override
                public void add( Object o )
                {
                    throw new UnsupportedOperationException( "Services list cannot be modified" );
                }
            };

        }

        @Nonnull
        @Override
        public List<Object> subList( int fromIndex, int toIndex )
        {
            return asList( toArray() ).subList( fromIndex, toIndex );
        }

        private synchronized void _addService( ServiceReference<Object> ref, Object service )
        {
            SortedSet<Pair<ServiceReference<Object>, Object>> newServices = new TreeSet<>( this.services );
            newServices.add( ImmutableComparablePair.<ServiceReference<Object>, Object>of( ref, service, SERVICE_PAIR_COMPARATOR ) );
            this.services = newServices;
        }

        private synchronized void _removeService( ServiceReference<?> ref, Object service )
        {
            SortedSet<Pair<ServiceReference<Object>, Object>> newServices = new TreeSet<>( this.services );
            Iterator<Pair<ServiceReference<Object>, Object>> iterator = newServices.iterator();
            while( iterator.hasNext() )
            {
                Pair<ServiceReference<Object>, Object> entry = iterator.next();
                ServiceReference<Object> entryRef = entry.getKey();
                if( entryRef != null )
                {
                    if( entryRef.equals( ref ) && service == entry.getValue() )
                    {
                        iterator.remove();
                        this.services = newServices;
                        return;
                    }
                }
            }
        }
    }
}
