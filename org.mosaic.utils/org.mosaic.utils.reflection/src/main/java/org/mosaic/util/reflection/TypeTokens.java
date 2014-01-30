package org.mosaic.util.reflection;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.reflect.TypeToken;
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

    @SuppressWarnings( "unchecked" )
    @Nonnull
    public static <T> TypeToken<T> of( @Nonnull Class<T> type )
    {
        return ( TypeToken<T> ) cache.getUnchecked( type );
    }

    @Nonnull
    public static TypeToken<?> of( @Nonnull Type type )
    {
        return cache.getUnchecked( type );
    }

    @Nonnull
    public static TypeToken<?> ofList( @Nonnull Type type )
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
