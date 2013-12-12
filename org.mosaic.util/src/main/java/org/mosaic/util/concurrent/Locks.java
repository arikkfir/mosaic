package org.mosaic.util.concurrent;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nonnull;

/**
 * @author arik
 */
public final class Locks
{
    @Nonnull
    private static final LoadingCache<String, Lock> locks =
            CacheBuilder.newBuilder()
                        .concurrencyLevel( 100 )
                        .initialCapacity( 100 )
                        .build( new CacheLoader<String, Lock>()
                        {
                            @Override
                            public Lock load( String key ) throws Exception
                            {
                                return new ReentrantLock();
                            }
                        } );

    @SuppressWarnings( "UnusedDeclaration" )
    public static Lock getLock( @Nonnull String name )
    {
        return Locks.locks.getUnchecked( name );
    }

    private Locks()
    {
    }
}
