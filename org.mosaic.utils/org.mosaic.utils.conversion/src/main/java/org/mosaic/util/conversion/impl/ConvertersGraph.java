package org.mosaic.util.conversion.impl;

import com.google.common.base.Objects;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Sets;
import com.google.common.primitives.Primitives;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.ExecutionError;
import com.google.common.util.concurrent.UncheckedExecutionException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import org.jgrapht.DirectedGraph;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.mosaic.util.conversion.ConversionException;
import org.mosaic.util.conversion.Converter;
import org.mosaic.util.reflection.TypeTokens;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;
import static org.jgrapht.alg.DijkstraShortestPath.findPathBetween;

/**
 * @author arik
 */
final class ConvertersGraph
{
    private static final Logger LOG = LoggerFactory.getLogger( ConvertersGraph.class );

    @Nonnull
    private static final Set<TypeToken<?>> ignoredTypes = Sets.<TypeToken<?>>newHashSet(
            TypeTokens.of( Object.class ),
            TypeTokens.of( Comparable.class ),
            TypeTokens.of( Serializable.class )
    );

    /**
     * A cache of converters for each specific source-to-target conversion pair.
     */
    @Nonnull
    private final LoadingCache<Pair<TypeToken<?>, TypeToken<?>>, Converter> conversionPathCache;

    /**
     * The graph of converters and the edges between them.
     */
    @Nullable
    private DirectedGraph<TypeToken<?>, ConverterAdapter> convertersGraph;

    ConvertersGraph()
    {
        this.conversionPathCache = CacheBuilder.newBuilder()
                                               .concurrencyLevel( 50 )
                                               .build( new CacheLoader<Pair<TypeToken<?>, TypeToken<?>>, Converter>()
                                               {
                                                   @Nonnull
                                                   @Override
                                                   public Converter load( @Nonnull Pair<TypeToken<?>, TypeToken<?>> key )
                                                           throws Exception
                                                   {
                                                       TypeToken<?> sourceTypeToken = key.getLeft();
                                                       TypeToken<?> targetTypeToken = key.getRight();
                                                       return findConverter( sourceTypeToken, targetTypeToken );
                                                   }
                                               } );
    }

    @Nonnull
    Converter getConverter( @Nonnull TypeToken<?> sourceTypeToken, @Nonnull TypeToken<?> targetTypeToken )
    {
        Pair<TypeToken<?>, TypeToken<?>> key = Pair.<TypeToken<?>, TypeToken<?>>of( sourceTypeToken, targetTypeToken );
        try
        {
            return this.conversionPathCache.get( key );
        }
        catch( ExecutionException | UncheckedExecutionException | ExecutionError e )
        {
            Throwable cause = e.getCause();
            if( cause instanceof ConversionException )
            {
                throw ( ConversionException ) cause;
            }
            else
            {
                throw new ConversionException( e.getMessage(), e, sourceTypeToken, targetTypeToken );
            }
        }
    }

    synchronized void addConverter( @Nonnull Converter converter )
    {
        SimpleDirectedGraph<TypeToken<?>, ConverterAdapter> graph = duplicateGraph();
        List<ConverterAdapter> adapters = addConverterAdapter( converter, graph );
        this.convertersGraph = graph;
        this.conversionPathCache.invalidateAll();
        for( ConverterAdapter adapter : adapters )
        {
            LOG.debug( "Discovered converter from '{}' to '{}' ({})", adapter.sourceTypeToken, adapter.targetTypeToken, adapter.converter );
        }
    }

    synchronized void removeConverter( @Nonnull Converter converter )
    {
        if( this.convertersGraph != null )
        {
            SimpleDirectedGraph<TypeToken<?>, ConverterAdapter> graph = duplicateGraph();
            for( ConverterAdapter adapter : this.convertersGraph.edgeSet() )
            {
                if( adapter.converter != converter )
                {
                    addConverterAdapter( adapter, graph );
                }
                else
                {
                    LOG.debug( "Removed converter from '{}' to '{}' ({})", adapter.sourceTypeToken, adapter.targetTypeToken, adapter.converter );
                    // not breaking, since there may be multiple adapters using the same converter (supertypes edges)
                }
            }
            this.conversionPathCache.invalidateAll();
            this.convertersGraph = graph;
        }
    }

    @Nonnull
    private SimpleDirectedGraph<TypeToken<?>, ConverterAdapter> duplicateGraph()
    {
        SimpleDirectedGraph<TypeToken<?>, ConverterAdapter> graph = new SimpleDirectedGraph<>( ConverterAdapter.class );
        if( this.convertersGraph != null )
        {
            Graphs.addGraph( graph, this.convertersGraph );
        }
        return graph;
    }

    @Nonnull
    private List<ConverterAdapter> addConverterAdapter( @Nonnull Converter converter,
                                                        @Nonnull Graph<TypeToken<?>, ConverterAdapter> graph )
    {
        TypeToken<? extends Converter> concreteTypeToken = TypeTokens.of( converter.getClass() );
        TypeToken<?> converterInterfaceTypeToken = concreteTypeToken.getSupertype( Converter.class );
        TypeVariable<? extends Class<?>>[] typeParameters = converterInterfaceTypeToken.getRawType().getTypeParameters();
        if( typeParameters == null || typeParameters.length != 2 )
        {
            throw new IllegalArgumentException( "illegal converter object - does not declare type variables for Converter interface" );
        }
        TypeToken<?> sourceTypeToken = converterInterfaceTypeToken.resolveType( typeParameters[ 0 ] );
        TypeToken<?> targetTypeToken = converterInterfaceTypeToken.resolveType( typeParameters[ 1 ] );

        // add target type, and all its supertypes including its implemented interfaces as vertexes (nodes) in the graph
        TypeToken<?>.TypeSet targetTypeTokens = targetTypeToken.getTypes();
        for( TypeToken<?> currentTargetTypeToken : targetTypeTokens )
        {
            if( !isIgnored( currentTargetTypeToken ) )
            {
                graph.addVertex( currentTargetTypeToken );
            }
        }

        // add source type, and all its supertypes including its implemented interfaces as vertexes (nodes) in the graph
        TypeToken<?>.TypeSet sourceTypeTokens = sourceTypeToken.getTypes();
        for( TypeToken<?> currentSourceTypeToken : sourceTypeTokens )
        {
            if( !isIgnored( currentSourceTypeToken ) )
            {
                graph.addVertex( currentSourceTypeToken );
            }
        }

        // add edge between this source type-token and all target type-tokens
        List<ConverterAdapter> adapters = new LinkedList<>();
        for( TypeToken<?> currentTargetTypeToken : targetTypeTokens )
        {
            if( !isIgnored( currentTargetTypeToken ) )
            {
                ConverterAdapter adapter = new ConverterAdapter( converter, sourceTypeToken, currentTargetTypeToken );
                graph.addEdge( sourceTypeToken, currentTargetTypeToken, adapter );
                adapters.add( adapter );
            }
        }
        return adapters;
    }

    @Nonnull
    private synchronized Converter findConverter( @Nonnull TypeToken<?> sourceType, @Nonnull TypeToken<?> targetType )
    {
        DirectedGraph<TypeToken<?>, ConverterAdapter> graph = this.convertersGraph;
        if( graph != null )
        {
            // do we have ANY converter, what-so-ever, that can generate target type token?
            if( !graph.containsVertex( targetType ) )
            {
                throw new ConversionException( "could not find a converter generating '" + targetType + "'", sourceType, targetType );
            }

            // use Dijkstra algorithm to find the shortest path between source and target from our known converters.
            // the code iterates all superclasses and implemented interfaces of source type, attempting to find a
            // path from each such type to our target type.
            //
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
            for( TypeToken<?> currentSourceTypeToken : sourceType.getTypes() )
            {
                if( graph.containsVertex( currentSourceTypeToken ) )
                {
                    try
                    {
                        List<ConverterAdapter> path = findPathBetween( graph, currentSourceTypeToken, targetType );
                        if( path != null )
                        {
                            return new CompositionConverter( path, sourceType, targetType );
                        }
                    }
                    catch( Exception e )
                    {
                        throw new ConversionException( e.getMessage(), e, sourceType, targetType );
                    }
                }
            }

            // if target type has a static 'valueOf(...)' method which accepts a single parameter, and that parameter
            // can accept instances of source type, return a converter that uses that method to convert
            for( Method method : Primitives.wrap( targetType.getRawType() ).getMethods() )
            {
                int modifiers = method.getModifiers();
                if( isStatic( modifiers ) && isPublic( modifiers ) && "valueOf".equals( method.getName() ) )
                {
                    Type[] genericParameterTypes = method.getGenericParameterTypes();
                    if( genericParameterTypes.length == 1 )
                    {
                        TypeToken<?> methodParameterType = TypeTokens.of( genericParameterTypes[ 0 ] );
                        if( methodParameterType.isAssignableFrom( sourceType ) )
                        {
                            return new ValueOfMethodConverter( method, sourceType, targetType );
                        }
                    }
                }
            }

            // no such path found
            throw new ConversionException( "no conversion path found", sourceType, targetType );
        }
        else
        {
            throw new ConversionException( "no converters registered", sourceType, targetType );
        }
    }

    private static boolean isIgnored( @Nonnull TypeToken<?> typeToken )
    {
        for( TypeToken<?> ignoredType : ignoredTypes )
        {
            if( ignoredType.getRawType().equals( typeToken.getRawType() ) )
            {
                return true;
            }
        }
        return false;
    }

    private class ConverterAdapter implements Converter
    {
        @Nonnull
        private final Converter converter;

        @Nonnull
        private final TypeToken<?> sourceTypeToken;

        @Nonnull
        private final TypeToken<?> targetTypeToken;

        private ConverterAdapter( @Nonnull Converter converter,
                                  @Nonnull TypeToken<?> sourceTypeToken,
                                  @Nonnull TypeToken<?> targetTypeToken )
        {
            this.converter = converter;
            this.sourceTypeToken = sourceTypeToken;
            this.targetTypeToken = targetTypeToken;
        }

        @SuppressWarnings( "unchecked" )
        @Nonnull
        @Override
        public Object convert( @Nonnull Object source )
        {
            return this.converter.convert( source );
        }

        @Override
        public String toString()
        {
            return Objects.toStringHelper( this )
                          .add( "source", this.sourceTypeToken )
                          .add( "target", this.targetTypeToken )
                          .add( "converter", this.converter )
                          .toString();
        }
    }

    private class CompositionConverter implements Converter
    {
        @Nonnull
        private final List<? extends Converter> converters;

        @Nonnull
        private final TypeToken<?> sourceTypeToken;

        @Nonnull
        private final TypeToken<?> targetTypeToken;

        private CompositionConverter( @Nonnull List<? extends Converter> converterAdapters,
                                      @Nonnull TypeToken<?> sourceTypeToken,
                                      @Nonnull TypeToken<?> targetTypeToken )
        {
            this.converters = new LinkedList<>( converterAdapters );
            this.sourceTypeToken = sourceTypeToken;
            this.targetTypeToken = targetTypeToken;
        }

        @SuppressWarnings( "unchecked" )
        @Nonnull
        @Override
        public Object convert( @Nonnull Object source )
        {
            Object value = source;
            for( Converter converter : this.converters )
            {
                value = converter.convert( value );
            }
            return value;
        }

        @Override
        public String toString()
        {
            return Objects.toStringHelper( this )
                          .add( "source", this.sourceTypeToken )
                          .add( "target", this.targetTypeToken )
                          .add( "converters", this.converters )
                          .toString();
        }
    }

    private class ValueOfMethodConverter implements Converter
    {
        @Nonnull
        private final Method method;

        @Nonnull
        private final TypeToken<?> sourceTypeToken;

        @Nonnull
        private final TypeToken<?> targetTypeToken;

        public ValueOfMethodConverter( @Nonnull Method method,
                                       @Nonnull TypeToken<?> sourceTypeToken,
                                       @Nonnull TypeToken<?> targetTypeToken )
        {

            this.method = method;
            this.sourceTypeToken = sourceTypeToken;
            this.targetTypeToken = targetTypeToken;
        }

        @Nonnull
        @Override
        public Object convert( @Nonnull Object source )
        {
            try
            {
                return this.method.invoke( null, source );
            }
            catch( InvocationTargetException e )
            {
                throw new ConversionException( "error in 'valueOf' method of target type", e, this.sourceTypeToken, this.targetTypeToken );
            }
            catch( Exception e )
            {
                throw new ConversionException( "unexpected conversion error", e, this.sourceTypeToken, this.targetTypeToken );
            }
        }

        @Override
        public String toString()
        {
            return Objects.toStringHelper( this )
                          .add( "source", this.sourceTypeToken )
                          .add( "target", this.targetTypeToken )
                          .add( "method", this.method )
                          .toString();
        }
    }
}
