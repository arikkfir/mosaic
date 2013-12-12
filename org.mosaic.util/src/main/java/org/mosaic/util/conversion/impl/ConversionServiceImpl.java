package org.mosaic.util.conversion.impl;

import com.google.common.base.Objects;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.primitives.Primitives;
import com.google.common.reflect.TypeToken;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jgrapht.DirectedGraph;
import org.jgrapht.Graph;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.mosaic.util.conversion.ConversionException;
import org.mosaic.util.conversion.ConversionService;
import org.mosaic.util.conversion.Converter;
import org.mosaic.util.pair.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;
import static org.jgrapht.alg.DijkstraShortestPath.findPathBetween;

/**
 * @author arik
 */
final class ConversionServiceImpl implements ConversionService
{
    private static final Logger LOG = LoggerFactory.getLogger( ConversionServiceImpl.class );

    @Nonnull
    private static final Byte ZERO_BYTE = ( byte ) 0;

    @Nonnull
    private static final Short ZERO_SHORT = ( short ) 0;

    @Nonnull
    private static final Integer ZERO_INT = 0;

    @Nonnull
    private static final Long ZERO_LONG = 0l;

    @Nonnull
    private static final Float ZERO_FLOAT = 0f;

    @Nonnull
    private static final Double ZERO_DOUBLE = 0d;

    @Nonnull
    private static final Character ZERO_CHAR = 0;

    @Nonnull
    private final Cache<Pair<TypeToken<?>, TypeToken<?>>, List<ConverterAdapter>> pathCache;

    @Nullable
    private DirectedGraph<TypeToken<?>, ConverterAdapter> convertersGraph;

    ConversionServiceImpl()
    {
        this.pathCache = CacheBuilder.newBuilder()
                                     .concurrencyLevel( 50 )
                                     .build();
    }

    public synchronized void close()
    {
        this.pathCache.invalidateAll();
        this.pathCache.cleanUp();
    }

    public synchronized void registerConverter( @Nonnull Converter<?, ?> converter )
    {
        SimpleDirectedGraph<TypeToken<?>, ConverterAdapter> graph = new SimpleDirectedGraph<>( ConverterAdapter.class );
        if( this.convertersGraph != null )
        {
            for( ConverterAdapter i : this.convertersGraph.edgeSet() )
            {
                addConverterAdapter( i, graph );
            }
        }

        ConverterAdapter adapter = new ConverterAdapter( converter );
        addConverterAdapter( adapter, graph );

        this.convertersGraph = graph;
        this.pathCache.invalidateAll();

        LOG.debug( "Registered converter from '{}' to '{}' (converter is {})", adapter.sourceTypeToken, adapter.targetTypeToken, adapter.converter );
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
    @Nullable
    @Override
    public <Source, Dest> Dest convert( @Nullable Source source, @Nonnull TypeToken<Dest> targetTypeToken )
    {
        if( source == null )
        {
            // if null and target-type is a primitive, custom translation is required
            if( targetTypeToken.isPrimitive() )
            {
                if( boolean.class.equals( targetTypeToken.getRawType() ) )
                {
                    return ( Dest ) Boolean.FALSE;
                }
                else if( byte.class.equals( targetTypeToken.getRawType() ) )
                {
                    return ( Dest ) ZERO_BYTE;
                }
                else if( short.class.equals( targetTypeToken.getRawType() ) )
                {
                    return ( Dest ) ZERO_SHORT;
                }
                else if( int.class.equals( targetTypeToken.getRawType() ) )
                {
                    return ( Dest ) ZERO_INT;
                }
                else if( long.class.equals( targetTypeToken.getRawType() ) )
                {
                    return ( Dest ) ZERO_LONG;
                }
                else if( double.class.equals( targetTypeToken.getRawType() ) )
                {
                    return ( Dest ) ZERO_DOUBLE;
                }
                else if( float.class.equals( targetTypeToken.getRawType() ) )
                {
                    return ( Dest ) ZERO_FLOAT;
                }
                else if( char.class.equals( targetTypeToken.getRawType() ) )
                {
                    return ( Dest ) ZERO_CHAR;
                }
                else
                {
                    throw new IllegalArgumentException( "unknown primitive type '" + targetTypeToken + "' and value is null" );
                }
            }
            else
            {
                // if null and target-type is a primitive, custom translation is required
                return null;
            }
        }

        // avoid conversion if not necessary
        TypeToken<?> sourceTypeToken = TypeToken.of( source.getClass() );
        if( targetTypeToken.isAssignableFrom( sourceTypeToken ) )
        {
            return ( Dest ) source;
        }

        // get conversion path from Source to Dest
        Pair<TypeToken<?>, TypeToken<?>> cacheKey = Pair.<TypeToken<?>, TypeToken<?>>of( sourceTypeToken, targetTypeToken );
        List<ConverterAdapter> path = this.pathCache.getIfPresent( cacheKey );
        if( path != null )
        {
            return ( Dest ) convert( source, path );
        }

        // first try to find a registered converter for the conversion path
        DirectedGraph<TypeToken<?>, ConverterAdapter> graph = this.convertersGraph;
        if( graph != null )
        {
            if( !graph.containsVertex( targetTypeToken ) )
            {
                throw new ConversionException( "could not find a converter generating '" + targetTypeToken + "'", sourceTypeToken, targetTypeToken );
            }

            // Some examples:
            //    REGISTER: converter<String,Dog>  (NOTE: Dog extends Animal)
            //    VERTIXES: String, CharSequence, Dog, Animal
            //       EDGES: String->Dog
            //              String->Animal
            //        TEST: convert(String(""),Dog):
            //                  check: String -> Dog            : OK
            //                  check: CharSequence -> Dog      : not found
            //                  check: Object -> Dog            : not found
            //        TEST: convert(CharSequence(""),Dog):
            //                  check: CharSequence -> Dog      : not found
            //                  check: Object -> Dog            : not found
            //        TEST: convert(String(""),Animal):
            //                  check: String -> Animal         : OK
            //                  check: CharSequence -> Animal   : not found
            //                  check: Object -> Animal         : not found
            //        TEST: convert(String(""),Animal):
            //                  check: String -> Animal         : OK
            //                  check: CharSequence -> Animal   : not found
            //                  check: Object -> Dog            : not found
            //
            //    REGISTER: converter<CharSequence,Dog>  (NOTE: Dog extends Animal)
            //    VERTIXES: CharSequence, Dog, Animal
            //       EDGES: CharSequence->Dog
            //              CharSequence->Animal
            //        TEST: convert(String(""),Dog):
            //                  check: String -> Dog            : not found
            //                  check: CharSequence -> Dog      : OK
            //                  check: Object -> Dog            : not found
            //        TEST: convert(CharSequence(""),Dog):
            //                  check: CharSequence -> Dog      : OK
            //                  check: Object -> Dog            : not found
            //        TEST: convert(String(""),Animal):
            //                  check: String -> Animal         : not found
            //                  check: CharSequence -> Animal   : OK
            //                  check: Object -> Animal         : not found
            for( TypeToken<?> currentSourceTypeToken : sourceTypeToken.getTypes() )
            {
                if( graph.containsVertex( currentSourceTypeToken ) )
                {
                    try
                    {
                        path = findPathBetween( graph, currentSourceTypeToken, targetTypeToken );
                    }
                    catch( Exception e )
                    {
                        throw new ConversionException( e.getMessage(), e, sourceTypeToken, targetTypeToken );
                    }

                    if( path != null )
                    {
                        // found converter - save the path in our cache
                        this.pathCache.put( cacheKey, path );
                        return ( Dest ) convert( source, path );
                    }
                }
            }
        }

        // if Dest has a static 'valueOf(Source)' method, use it
        Class<? super Dest> targetType = Primitives.wrap( targetTypeToken.getRawType() );
        for( Method method : targetType.getMethods() )
        {
            int modifiers = method.getModifiers();
            if( isStatic( modifiers ) && isPublic( modifiers ) && "valueOf".equals( method.getName() ) )
            {
                Type[] genericParameterTypes = method.getGenericParameterTypes();
                if( genericParameterTypes.length == 1 )
                {
                    TypeToken<?> methodParameterType = TypeToken.of( genericParameterTypes[ 0 ] );
                    if( methodParameterType.isAssignableFrom( sourceTypeToken ) )
                    {
                        try
                        {
                            return ( Dest ) method.invoke( null, source );
                        }
                        catch( IllegalAccessException e )
                        {
                            throw new ConversionException( "illegal access", e, sourceTypeToken, targetTypeToken );
                        }
                        catch( InvocationTargetException e )
                        {
                            throw new ConversionException( "error in 'valueOf' method of target type", e, sourceTypeToken, targetTypeToken );
                        }
                    }
                }
            }
        }
        throw new ConversionException( "no conversion path found", sourceTypeToken, targetTypeToken );
    }

    @Nullable
    @Override
    public <Source, Dest> Dest convert( @Nullable Source source, @Nonnull Class<Dest> targetTypeToken )
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

    private void addConverterAdapter( @Nonnull ConverterAdapter adapter,
                                      @Nonnull Graph<TypeToken<?>, ConverterAdapter> graph )
    {
        // add source type, and all its supertypes including its implemented interfaces as vertexes (nodes) in the graph
        for( TypeToken<?> currentSourceTypeToken : adapter.sourceTypeToken.getTypes() )
        {
            graph.addVertex( currentSourceTypeToken );
        }

        // add target type, and all its supertypes including its implemented interfaces as vertexes (nodes) in the graph
        // add edges between source type-token and the target type-token, and all its supertypes and interfaces
        for( TypeToken<?> currentTargetTypeToken : adapter.targetTypeToken.getTypes() )
        {
            graph.addVertex( currentTargetTypeToken );
        }
        graph.addEdge( adapter.sourceTypeToken, adapter.targetTypeToken, adapter );
    }

    private class ConverterAdapter
    {
        @Nonnull
        private final Converter converter;

        @Nonnull
        private final TypeToken<?> sourceTypeToken;

        @Nonnull
        private final TypeToken<?> targetTypeToken;

        @SuppressWarnings( "unchecked" )
        private ConverterAdapter( @Nonnull Converter<?, ?> converter )
        {
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
                          .toString();
        }
    }
}
