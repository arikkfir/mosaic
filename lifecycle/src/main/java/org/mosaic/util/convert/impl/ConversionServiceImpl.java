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
import org.osgi.framework.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import static org.jgrapht.alg.DijkstraShortestPath.findPathBetween;
import static org.osgi.framework.Constants.OBJECTCLASS;

/**
 * @author arik
 */
public class ConversionServiceImpl implements ConversionService, ServiceListener, DisposableBean
{
    private static final Logger LOG = LoggerFactory.getLogger( ConversionServiceImpl.class );

    @Nullable
    private ServiceRegistration<ConversionService> serviceRegistration;

    @Nonnull
    private final AtomicLong customIdGenerator = new AtomicLong( 0 );

    @Nullable
    private final BundleContext bundleContext;

    @Nonnull
    private final Cache<Pair<TypeToken<?>, TypeToken<?>>, List<ConverterAdapter>> pathCache;

    @Nullable
    private DirectedGraph<TypeToken<?>, ConverterAdapter> convertersGraph;

    public ConversionServiceImpl() throws InvalidSyntaxException
    {
        this( null );
    }

    public ConversionServiceImpl( @Nullable BundleContext bundleContext ) throws InvalidSyntaxException
    {
        this.bundleContext = bundleContext;
        this.pathCache = CacheBuilder.newBuilder()
                                     .concurrencyLevel( 50 )
                                     .build();

        if( this.bundleContext != null )
        {
            bundleContext.addServiceListener( this, "(" + OBJECTCLASS + "=" + Converter.class + ")" );
            this.serviceRegistration = ServiceUtils.register( this.bundleContext, ConversionService.class, this );
        }
    }

    @Override
    public void destroy() throws Exception
    {
        this.pathCache.invalidateAll();
        this.pathCache.cleanUp();

        if( this.bundleContext != null && this.serviceRegistration != null )
        {
            this.serviceRegistration = ServiceUtils.unregister( this.serviceRegistration );
            this.serviceRegistration = null;
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

    @SuppressWarnings("unchecked")
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
        throw new ConversionException( "no conversion path found", sourceTypeToken, targetTypeToken, source );
    }

    @Nonnull
    @Override
    public <Source, Dest> Dest convert( @Nonnull Source source, @Nonnull Class<Dest> targetTypeToken )
    {
        return convert( source, TypeToken.of( targetTypeToken ) );
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized void serviceChanged( ServiceEvent event )
    {
        if( this.bundleContext == null )
        {
            return;
        }

        if( event.getType() == ServiceEvent.REGISTERED )
        {
            registerConverter( new ConverterAdapter( this.bundleContext, ( ServiceReference<Converter<?, ?>> ) event.getServiceReference() ) );
        }
        else if( event.getType() == ServiceEvent.UNREGISTERING )
        {
            if( this.convertersGraph != null )
            {
                ServiceReference<?> reference = event.getServiceReference();
                long id = ServiceUtils.getId( reference );

                DirectedGraph<TypeToken<?>, ConverterAdapter> graph = new SimpleDirectedGraph<>( ConverterAdapter.class );
                for( ConverterAdapter i : this.convertersGraph.edgeSet() )
                {
                    if( i.id != id )
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
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    private Object convert( @Nonnull Object source, @Nonnull List<ConverterAdapter> path )
    {
        // convert using the found path
        Object currentSource = source;
        for( ConverterAdapter entry : path )
        {
            currentSource = entry.converter.convert( currentSource );
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

        private ConverterAdapter( @Nonnull BundleContext bundleContext,
                                  @Nonnull ServiceReference<Converter<?, ?>> reference )
        {
            this( ServiceUtils.getId( reference ), bundleContext.getService( reference ) );
        }

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
