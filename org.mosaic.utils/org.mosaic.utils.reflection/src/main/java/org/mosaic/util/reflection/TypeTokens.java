package org.mosaic.util.reflection;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.UncheckedExecutionException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import javax.annotation.Nonnull;

/**
 * @author arik
 */
public class TypeTokens
{
    @Nonnull
    private static final LoadingCache<Type, TypeToken<?>> cache =
            CacheBuilder.newBuilder()
                        .concurrencyLevel( 10 )
                        .build( new CacheLoader<Type, TypeToken<?>>()
                        {
                            @Nonnull
                            @Override
                            public TypeToken<?> load( @Nonnull Type key ) throws Exception
                            {
                                return TypeToken.of( key );
                            }
                        } );

    @Nonnull
    public static final TypeToken<String> STRING = TypeTokens.of( String.class );

    @Nonnull
    public static final TypeToken<Boolean> BOOLEAN = TypeTokens.of( Boolean.class );

    @Nonnull
    public static final TypeToken<List<String>> STRING_LIST = new TypeToken<List<String>>()
    {
    };

    @SuppressWarnings("unchecked")
    @Nonnull
    public static <T> TypeToken<T> of( @Nonnull Class<T> type )
    {
        try
        {
            return ( TypeToken<T> ) cache.getUnchecked( type );
        }
        catch( UncheckedExecutionException e )
        {
            Throwable cause = e.getCause();
            if( cause instanceof RuntimeException )
            {
                throw ( RuntimeException ) cause;
            }
            else
            {
                throw e;
            }
        }
    }

    @Nonnull
    public static TypeToken<?> of( @Nonnull Type type )
    {
        try
        {
            return cache.getUnchecked( type );
        }
        catch( UncheckedExecutionException e )
        {
            Throwable cause = e.getCause();
            if( cause instanceof RuntimeException )
            {
                throw ( RuntimeException ) cause;
            }
            else
            {
                throw e;
            }
        }
    }

    @Nonnull
    public static TypeToken<?> ofListItemType( @Nonnull Type type )
    {
        TypeToken<?> listToken = TypeTokens.of( type );
        try
        {
            Method method = List.class.getMethod( "get", int.class );
            return listToken.resolveType( method.getGenericReturnType() );
        }
        catch( NoSuchMethodException e )
        {
            throw new IllegalStateException();
        }
    }

    public static void clearCaches()
    {
        cache.invalidateAll();
    }
}
