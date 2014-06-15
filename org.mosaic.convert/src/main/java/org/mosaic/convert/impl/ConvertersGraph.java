package org.mosaic.convert.impl;

import com.fasterxml.classmate.*;
import com.fasterxml.classmate.members.HierarchicType;
import com.fasterxml.classmate.members.ResolvedMethod;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import org.jgrapht.DirectedGraph;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.mosaic.convert.ConversionException;
import org.mosaic.convert.Converter;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.core.util.base.ToStringHelper;
import org.mosaic.core.util.concurrency.ReadWriteLock;
import org.mosaic.core.util.logging.Logging;
import org.slf4j.Logger;

import static java.util.Collections.unmodifiableSet;
import static org.jgrapht.alg.DijkstraShortestPath.findPathBetween;

/**
 * @author arik
 */
final class ConvertersGraph
{
    private static final Logger LOG = Logging.getLogger();

    @Nonnull
    private static final Set<Class<?>> ignoredTypes = unmodifiableSet( new HashSet<>( Arrays.<Class<?>>asList(
            Object.class,
            Comparable.class,
            Serializable.class
    ) ) );

    private static final AnnotationConfiguration.StdConfiguration ANNOTATION_CONFIG = new AnnotationConfiguration.StdConfiguration( AnnotationInclusion.INCLUDE_AND_INHERIT_IF_INHERITED );

    @Nonnull
    private static ResolvedType wrapIfPrimitive( @Nonnull TypeResolver typeResolver,
                                                 @Nonnull ResolvedType resolvedType )
    {
        if( resolvedType.isPrimitive() )
        {
            Class<?> erasedType = resolvedType.getErasedType();
            if( boolean.class.equals( erasedType ) )
            {
                return typeResolver.resolve( Boolean.class );
            }
            else if( byte.class.equals( erasedType ) )
            {
                return typeResolver.resolve( Byte.class );
            }
            else if( short.class.equals( erasedType ) )
            {
                return typeResolver.resolve( Short.class );
            }
            else if( char.class.equals( erasedType ) )
            {
                return typeResolver.resolve( Character.class );
            }
            else if( int.class.equals( erasedType ) )
            {
                return typeResolver.resolve( Integer.class );
            }
            else if( float.class.equals( erasedType ) )
            {
                return typeResolver.resolve( Float.class );
            }
            else if( double.class.equals( erasedType ) )
            {
                return typeResolver.resolve( Double.class );
            }
            else
            {
                throw new IllegalArgumentException( "not a primitive: " + resolvedType );
            }
        }
        else
        {
            return resolvedType;
        }
    }

    @Nonnull
    private static Collection<ResolvedType> getTypes( @Nonnull ResolvedType type )
    {
        Set<ResolvedType> types = new LinkedHashSet<>();

        ResolvedType currentType = type;
        while( currentType != null )
        {
            types.add( currentType );
            types.addAll( type.getImplementedInterfaces() );
            currentType = currentType.getParentClass();
        }
        return types;
    }

    private static boolean isIgnored( @Nonnull Class<?> typeToken )
    {
        for( Class<?> ignoredType : ignoredTypes )
        {
            if( ignoredType.equals( typeToken ) )
            {
                return true;
            }
        }
        return false;
    }

    private static class ConverterAdapter<Source, Dest> implements Converter<Source, Dest>
    {
        @Nonnull
        private final Converter<Source, Dest> converter;

        @Nonnull
        private final Class<Source> sourceTypeToken;

        @Nonnull
        private final Class<Dest> targetTypeToken;

        private ConverterAdapter( @Nonnull Converter<Source, Dest> converter,
                                  @Nonnull Class<Source> sourceTypeToken,
                                  @Nonnull Class<Dest> targetTypeToken )
        {
            this.converter = converter;
            this.sourceTypeToken = sourceTypeToken;
            this.targetTypeToken = targetTypeToken;
        }

        @Nonnull
        @Override
        public Dest convert( @Nonnull Source source )
        {
            return this.converter.convert( source );
        }

        @Override
        public String toString()
        {
            return ToStringHelper.create( this )
                                 .add( "source", this.sourceTypeToken )
                                 .add( "target", this.targetTypeToken )
                                 .add( "converter", this.converter )
                                 .toString();
        }
    }

    private static class CompositionConverter<Source, Dest> implements Converter<Source, Dest>
    {
        @Nonnull
        private final List<? extends Converter> converters;

        @Nonnull
        private final Class<Source> sourceTypeToken;

        @Nonnull
        private final Class<Dest> targetTypeToken;

        private CompositionConverter( @Nonnull List<? extends Converter> converterAdapters,
                                      @Nonnull Class<Source> sourceTypeToken,
                                      @Nonnull Class<Dest> targetTypeToken )
        {
            this.converters = new LinkedList<>( converterAdapters );
            this.sourceTypeToken = sourceTypeToken;
            this.targetTypeToken = targetTypeToken;
        }

        @SuppressWarnings( "unchecked" )
        @Nonnull
        @Override
        public Dest convert( @Nonnull Source source )
        {
            Object value = source;
            for( Converter converter : this.converters )
            {
                value = converter.convert( value );
            }
            return this.targetTypeToken.cast( value );
        }

        @Override
        public String toString()
        {
            return ToStringHelper.create( this )
                                 .add( "source", this.sourceTypeToken )
                                 .add( "target", this.targetTypeToken )
                                 .add( "converters", this.converters )
                                 .toString();
        }
    }

    private static class ValueOfMethodConverter<Source, Dest> implements Converter<Source, Dest>
    {
        @Nonnull
        private final Method method;

        @Nonnull
        private final Class<Source> sourceTypeToken;

        @Nonnull
        private final Class<Dest> targetTypeToken;

        public ValueOfMethodConverter( @Nonnull Method method,
                                       @Nonnull Class<Source> sourceTypeToken,
                                       @Nonnull Class<Dest> targetTypeToken )
        {

            this.method = method;
            this.sourceTypeToken = sourceTypeToken;
            this.targetTypeToken = targetTypeToken;
        }

        @Nonnull
        @Override
        public Dest convert( @Nonnull Source source )
        {
            try
            {
                return this.targetTypeToken.cast( this.method.invoke( null, source ) );
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
            return ToStringHelper.create( this )
                                 .add( "source", this.sourceTypeToken )
                                 .add( "target", this.targetTypeToken )
                                 .add( "method", this.method )
                                 .toString();
        }
    }

    @Nonnull
    private final ReadWriteLock lock = new ReadWriteLock( "ConvertersGraphLock" );

    /**
     * A cache of converters for each specific source-to-target conversion pair.
     */
    @Nonnull
    private final Map<Class<?>, Map<Class<?>, Converter<?, ?>>> conversionPathCache;

    /**
     * The graph of converters and the edges between them.
     */
    @Nullable
    private DirectedGraph<Class<?>, ConverterAdapter> convertersGraph;

    ConvertersGraph()
    {
        this.conversionPathCache = new HashMap<>( 1000 );
    }

    @Nonnull
    Converter getConverter( @Nonnull Class<?> sourceTypeToken, @Nonnull Class<?> targetTypeToken )
    {
        this.lock.acquireReadLock();
        try
        {
            Map<Class<?>, Converter<?, ?>> targetsMap = this.conversionPathCache.get( sourceTypeToken );
            if( targetsMap == null )
            {
                this.lock.releaseReadLock();
                this.lock.acquireWriteLock();
                try
                {
                    targetsMap = new HashMap<>( 100 );
                    this.conversionPathCache.put( sourceTypeToken, targetsMap );
                }
                finally
                {
                    this.lock.releaseWriteLock();
                    this.lock.acquireReadLock();
                }
            }

            Converter<?, ?> converter = targetsMap.get( targetTypeToken );
            if( converter == null )
            {
                this.lock.releaseReadLock();
                this.lock.acquireWriteLock();
                try
                {
                    converter = findConverter( sourceTypeToken, targetTypeToken );
                    targetsMap.put( targetTypeToken, converter );
                }
                finally
                {
                    this.lock.releaseWriteLock();
                    this.lock.acquireReadLock();
                }
            }

            return converter;
        }
        catch( ConversionException e )
        {
            throw e;
        }
        catch( Throwable e )
        {
            throw new ConversionException( e.getMessage(), e, sourceTypeToken, targetTypeToken );
        }
        finally
        {
            this.lock.releaseReadLock();
        }
    }

    void addConverter( @Nonnull Converter<?, ?> converter )
    {
        this.lock.write( () -> {
            SimpleDirectedGraph<Class<?>, ConverterAdapter> graph = duplicateGraph();
            List<ConverterAdapter> adapters = addConverterAdapter( converter, graph );
            this.convertersGraph = graph;
            this.conversionPathCache.clear();
            for( ConverterAdapter adapter : adapters )
            {
                LOG.debug( "Added converter from '{}' to '{}' ({})", adapter.sourceTypeToken, adapter.targetTypeToken, adapter.converter );
            }
        } );
    }

    void removeConverter( @Nonnull Converter converter )
    {
        this.lock.write( () -> {
            if( this.convertersGraph != null )
            {
                SimpleDirectedGraph<Class<?>, ConverterAdapter> graph = duplicateGraph();
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
                this.conversionPathCache.clear();
                this.convertersGraph = graph;
            }
        } );
    }

    @Nonnull
    private SimpleDirectedGraph<Class<?>, ConverterAdapter> duplicateGraph()
    {
        SimpleDirectedGraph<Class<?>, ConverterAdapter> graph = new SimpleDirectedGraph<>( ConverterAdapter.class );
        if( this.convertersGraph != null )
        {
            Graphs.addGraph( graph, this.convertersGraph );
        }
        return graph;
    }

    @Nonnull
    private List<ConverterAdapter> addConverterAdapter( @Nonnull Converter<?, ?> converter,
                                                        @Nonnull Graph<Class<?>, ConverterAdapter> graph )
    {
        // resolve generics for converter type
        ResolvedType converterType = new TypeResolver().resolve( converter.getClass() );

        // discover generic type parameters for Converter interface in the given converter's type
        List<ResolvedType> converterParameters = converterType.typeParametersFor( Converter.class );
        if( converterParameters == null || converterParameters.size() != 2 )
        {
            throw new IllegalArgumentException( "converter " + converter + " has no type parameters for Converter interface" );
        }
        ResolvedType sourceType = converterParameters.get( 0 );
        ResolvedType targetType = converterParameters.get( 1 );

        // add target type, and all its supertypes including its implemented interfaces as vertexes (nodes) in the graph
        getTypes( targetType ).stream()
                              .filter( type -> !isIgnored( type.getErasedType() ) )
                              .forEach( type -> graph.addVertex( type.getErasedType() ) );

        // add source type, and all its supertypes including its implemented interfaces as vertexes (nodes) in the graph
        getTypes( sourceType ).stream()
                              .filter( type -> !isIgnored( type.getErasedType() ) )
                              .forEach( type -> graph.addVertex( type.getErasedType() ) );

        // add edge between this source type-token and all target type-tokens
        List<ConverterAdapter> adapters = new LinkedList<>();
        getTypes( targetType ).stream()
                              .filter( type -> !isIgnored( type.getErasedType() ) )
                              .forEach( currentTargetType -> {
                                  Class<?> erasedSourceType = sourceType.getErasedType();
                                  Class<?> erasedTargetType = currentTargetType.getErasedType();

                                  @SuppressWarnings( "unchecked" )
                                  ConverterAdapter adapter = new ConverterAdapter( converter, erasedSourceType, erasedTargetType );
                                  graph.addEdge( erasedSourceType, erasedTargetType, adapter );
                                  adapters.add( adapter );
                              } );

        return adapters;
    }

    @SuppressWarnings( "unchecked" )
    @Nonnull
    private Converter findConverter( @Nonnull Class<?> sourceType, @Nonnull Class<?> targetType )
    {
        DirectedGraph<Class<?>, ConverterAdapter> graph = this.convertersGraph;
        if( graph != null )
        {
            // do we have ANY converter, what-so-ever, that can generate target type token?
            if( !graph.containsVertex( targetType ) )
            {
                throw new ConversionException( "could not find a converter generating '" + targetType + "'", sourceType, targetType );
            }

            // resolve types
            TypeResolver typeResolver = new TypeResolver();
            MemberResolver memberResolver = new MemberResolver( typeResolver );

            ResolvedType resolvedSourceType = wrapIfPrimitive( typeResolver, typeResolver.resolve( sourceType ) );
            ResolvedTypeWithMembers resolvedSourceTypeWithMembers = memberResolver.resolve( resolvedSourceType, ANNOTATION_CONFIG, null );

            ResolvedType resolvedTargetType = wrapIfPrimitive( typeResolver, typeResolver.resolve( targetType ) );
            ResolvedTypeWithMembers resolvedTargetTypeWithMembers = memberResolver.resolve( resolvedTargetType, ANNOTATION_CONFIG, null );

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
            for( HierarchicType type : resolvedSourceTypeWithMembers.allTypesAndOverrides() )
            {
                if( graph.containsVertex( type.getErasedType() ) )
                {
                    try
                    {
                        List<ConverterAdapter> path = findPathBetween( graph, type.getErasedType(), targetType );
                        if( path != null )
                        {
                            //noinspection unchecked
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
            for( ResolvedMethod method : resolvedTargetTypeWithMembers.getStaticMethods() )
            {
                if( method.isStatic() && method.isPublic() && "valueOf".equals( method.getName() ) )
                {
                    if( method.getArgumentCount() == 1 )
                    {
                        ResolvedType argumentType = method.getArgumentType( 0 );
                        if( argumentType.getErasedType().isAssignableFrom( sourceType ) )
                        {
                            return new ValueOfMethodConverter( method.getRawMember(), sourceType, targetType );
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
}
