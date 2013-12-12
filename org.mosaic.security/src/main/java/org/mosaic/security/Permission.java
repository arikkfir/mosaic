package org.mosaic.security;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.*;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnull;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;

/**
 * @author arik
 */
public final class Permission
{
    @Nonnull
    private static final LoadingCache<String, Permission> permissionsCache =
            CacheBuilder.newBuilder()
                        .concurrencyLevel( 100 )
                        .initialCapacity( 10000 )
                        .maximumSize( 1000000 )
                        .softValues()
                        .build( new CacheLoader<String, Permission>()
                        {
                            @Nonnull
                            @Override
                            public Permission load( @Nonnull String key ) throws Exception
                            {
                                return new Permission( key );
                            }
                        } );

    @Nonnull
    public static Permission get( @Nonnull String value )
    {
        try
        {
            return Permission.permissionsCache.get( value );
        }
        catch( ExecutionException e )
        {
            Throwable cause = e.getCause();
            throw new IllegalPermissionSyntaxException( cause.getMessage(), cause, value );
        }
    }

    @Nonnull
    private final List<Set<String>> tokens;

    private Permission( @Nonnull String value )
    {
        List<Set<String>> tokens = new LinkedList<>();
        for( String token : value.split( ":" ) )
        {
            Set<String> items = new LinkedHashSet<>();

            List<String> allItems = asList( token.split( "," ) );
            if( allItems.contains( "*" ) )
            {
                items.add( "*" );
            }
            else
            {
                items.addAll( allItems );
            }
            tokens.add( unmodifiableSet( items ) );
        }
        this.tokens = unmodifiableList( tokens );
    }

    public boolean implies( @Nonnull Permission permission )
    {
        Iterator<Set<String>> thisIterator = this.tokens.iterator();
        Iterator<Set<String>> thatIterator = permission.tokens.iterator();
        while( thisIterator.hasNext() && thatIterator.hasNext() )
        {
            Set<String> thisItems = thisIterator.next();
            Set<String> thatItems = thatIterator.next();
            if( !implied( thisItems, thatItems ) )
            {
                return false;
            }
        }
        return thatIterator.hasNext();
    }

    private boolean implied( @Nonnull Set<String> available, @Nonnull Set<String> required )
    {
        if( !available.contains( "*" ) )
        {
            for( String requiredItem : required )
            {
                if( !available.contains( requiredItem ) )
                {
                    return false;
                }
            }
        }
        return true;
    }
}
