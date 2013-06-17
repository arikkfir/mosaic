package org.mosaic.util.convert.impl;

import com.google.common.base.Objects;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.reflect.TypeToken;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jgrapht.DirectedGraph;
import org.jgrapht.Graph;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.mosaic.lifecycle.impl.util.ServiceUtils;
import org.mosaic.util.convert.ConversionException;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.util.convert.Converter;
import org.mosaic.util.pair.ImmutablePair;
import org.mosaic.util.pair.Pair;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import static org.jgrapht.alg.DijkstraShortestPath.findPathBetween;

/**
 * @author arik
 */
public class ConversionServiceImpl implements ConversionService, InitializingBean, DisposableBean
{
    private static final Logger LOG = LoggerFactory.getLogger( ConversionServiceImpl.class );

    @Nonnull
    private final AtomicLong customIdGenerator = new AtomicLong( 0 );

    @Nullable
    private final BundleContext bundleContext;

    @Nonnull
    private final Cache<Pair<TypeToken<?>, TypeToken<?>>, List<ConverterAdapter>> pathCache;

    @Nullable
    private final ServiceTracker<Converter, Converter> convertersTracker;

    @Nullable
    private ServiceRegistration<ConversionService> serviceRegistration;

    @Nullable
    private DirectedGraph<TypeToken<?>, ConverterAdapter> convertersGraph;

    public ConversionServiceImpl() throws InvalidSyntaxException
    {
        this( null );
    }

    public ConversionServiceImpl( @Nullable BundleContext bundleContext ) throws InvalidSyntaxException
    {
        this.bundleContext = bundleContext;
        if( this.bundleContext != null )
        {
            this.convertersTracker = new ServiceTracker<>(
                    this.bundleContext,
                    Converter.class,
                    new ServiceTrackerCustomizer<Converter, Converter>()
                    {
                        @Override
                        public synchronized Converter addingService( ServiceReference<Converter> reference )
                        {
                            BundleContext bundleContext = ConversionServiceImpl.this.bundleContext;
                            if( bundleContext != null )
                            {
                                Converter<?, ?> converter = bundleContext.getService( reference );
                                if( converter != null )
                                {
                                    long id = ServiceUtils.getId( reference );
                                    registerConverter( new ConverterAdapter( id, converter ) );
                                    return converter;
                                }
                            }
                            return null;
                        }

                        @Override
                        public void modifiedService( ServiceReference<Converter> reference, Converter service )
                        {
                            // no-op
                        }

                        @Override
                        public synchronized void removedService( ServiceReference<Converter> reference,
                                                                 Converter service )
                        {
                            unregisterConverter( service );
                        }
                    }

            );
        }
        else
        {
            this.convertersTracker = null;
        }
        this.pathCache = CacheBuilder.newBuilder()
                                     .concurrencyLevel( 50 )
                                     .build();
    }

    @Override
    public synchronized void afterPropertiesSet() throws Exception
    {
        if( this.convertersTracker != null )
        {
            this.convertersTracker.open();
        }

        if( this.bundleContext != null )
        {
            this.serviceRegistration = ServiceUtils.register( this.bundleContext, ConversionService.class, this );
        }
    }

    @Override
    public synchronized void destroy() throws Exception
    {
        if( this.bundleContext != null && this.serviceRegistration != null )
        {
            this.serviceRegistration = ServiceUtils.unregister( this.serviceRegistration );
        }

        this.pathCache.invalidateAll();
        this.pathCache.cleanUp();

        if( this.convertersTracker != null )
        {
            this.convertersTracker.close();
        }
    }

    public synchronized void registerConverter( @Nonnull Converter<?, ?> converter )
    {
        registerConverter( new ConverterAdapter( converter ) );
    }

    public synchronized void unregisterConverter( @Nonnull Converter<?, ?> converter )
    {
        if( this.convertersGraph != null )
        {
            DirectedGraph<TypeToken<?>, ConverterAdapter> graph = new SimpleDirectedGraph<>( ConverterAdapter.class );
            for( ConverterAdapter i : this.convertersGraph.edgeSet() )
            {
                if( i.converter != converter )
                {
                    addConverterAdapter( i, graph );
                }
                else
                {
                    LOG.debug( "Unregistered converter from '{}' to '{}' (converter is {})", i.sourceTypeToken, i.targetTypeToken, i.converter );
                }
            }
            this.convertersGraph = graph;
            this.pathCache.invalidateAll();
        }
    }

    @SuppressWarnings( "unchecked" )
    @Nonnull
    @Override
    public <Source, Dest> Dest convert( @Nonnull Source source, @Nonnull TypeToken<Dest> targetTypeToken )
    {
        TypeToken<?> sourceTypeToken = TypeToken.of( source.getClass() );

        ImmutablePair<TypeToken<?>, TypeToken<?>> cacheKey = ImmutablePair.<TypeToken<?>, TypeToken<?>>of( sourceTypeToken, targetTypeToken );
        List<ConverterAdapter> path = this.pathCache.getIfPresent( cacheKey );
        if( path != null )
        {
            return ( Dest ) convert( source, path );
        }

        DirectedGraph<TypeToken<?>, ConverterAdapter> graph = this.convertersGraph;
        if( graph != null )
        {
            for( TypeToken<?> typeToken : sourceTypeToken.getTypes() )
            {
                if( graph.containsVertex( typeToken ) )
                {
                    path = findPathBetween( graph, typeToken, targetTypeToken );
                    if( path != null )
                    {
                        // found converter - save the path in our cache
                        this.pathCache.put( cacheKey, path );
                        return ( Dest ) convert( source, path );
                    }
                }
            }
        }
        throw new ConversionException( "no conversion path found", sourceTypeToken, targetTypeToken );
    }

    @Nonnull
    @Override
    public <Source, Dest> Dest convert( @Nonnull Source source, @Nonnull Class<Dest> targetTypeToken )
    {
        return convert( source, TypeToken.of( targetTypeToken ) );
    }

    @SuppressWarnings( "unchecked" )
    @Nonnull
    private Object convert( @Nonnull Object source, @Nonnull List<ConverterAdapter> path )
    {
        // convert using the found path
        Object currentSource = source;
        for( ConverterAdapter entry : path )
        {
            try
            {
                currentSource = entry.converter.convert( currentSource );
            }
            catch( Exception e )
            {
                throw new ConversionException( e.getMessage(), e, entry.sourceTypeToken, entry.targetTypeToken );
            }
        }
        return currentSource;
    }

    private synchronized void registerConverter( @Nonnull ConverterAdapter adapter )
    {
        DirectedGraph<TypeToken<?>, ConverterAdapter> graph = new SimpleDirectedGraph<>( ConverterAdapter.class );
        if( this.convertersGraph != null )
        {
            for( ConverterAdapter i : this.convertersGraph.edgeSet() )
            {
                addConverterAdapter( i, graph );
            }
        }
        addConverterAdapter( adapter, graph );

        this.convertersGraph = graph;
        this.pathCache.invalidateAll();

        LOG.debug( "Registered converter from '{}' to '{}' (converter is {})", adapter.sourceTypeToken, adapter.targetTypeToken, adapter.converter );
    }

    private void addConverterAdapter( @Nonnull ConverterAdapter adapter,
                                      @Nonnull Graph<TypeToken<?>, ConverterAdapter> graph )
    {
        addVertex( adapter.sourceTypeToken, graph );
        addVertex( adapter.targetTypeToken, graph );
        graph.addEdge( adapter.sourceTypeToken, adapter.targetTypeToken, adapter );
    }

    private void addVertex( @Nonnull TypeToken<?> type, @Nonnull Graph<TypeToken<?>, ConverterAdapter> graph )
    {
        for( TypeToken<?> typeToken : type.getTypes() )
        {
            graph.addVertex( typeToken );
        }
    }

    private class ConverterAdapter
    {
        private final long id;

        @Nonnull
        private final Converter converter;

        @Nonnull
        private final TypeToken<?> sourceTypeToken;

        @Nonnull
        private final TypeToken<?> targetTypeToken;

        private ConverterAdapter( @Nonnull Converter<?, ?> converter )
        {
            this( customIdGenerator.decrementAndGet(), converter );
        }

        private ConverterAdapter( long id, @Nonnull Converter<?, ?> converter )
        {
            this.id = id;
            this.converter = converter;

            TypeToken<? extends Converter> concreteTypeToken = TypeToken.of( this.converter.getClass() );
            TypeToken<?> converterInterfaceTypeToken = concreteTypeToken.getSupertype( Converter.class );
            Class<?> converterClass = converterInterfaceTypeToken.getRawType();
            this.sourceTypeToken = converterInterfaceTypeToken.resolveType( converterClass.getTypeParameters()[ 0 ] );
            this.targetTypeToken = converterInterfaceTypeToken.resolveType( converterClass.getTypeParameters()[ 1 ] );
        }

        @Override
        public String toString()
        {
            return Objects.toStringHelper( this )
                          .add( "source", this.sourceTypeToken )
                          .add( "target", this.targetTypeToken )
                          .addValue( this.id ).toString();
        }
    }
}
