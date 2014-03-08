package org.mosaic.util.reflection;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.thoughtworks.paranamer.AdaptiveParanamer;
import com.thoughtworks.paranamer.BytecodeReadingParanamer;
import com.thoughtworks.paranamer.PositionalParanamer;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;

import static java.util.Arrays.asList;

/**
 * @author arik
 */
public final class MethodParameterNames
{
    @Nonnull
    private static final AdaptiveParanamer paranamer = new AdaptiveParanamer(
            new BytecodeReadingParanamer(),
            new PositionalParanamer()
    );

    @Nonnull
    private static final LoadingCache<Method, List<String>> cache =
            CacheBuilder.newBuilder()
                        .concurrencyLevel( 10 )
                        .build( new CacheLoader<Method, List<String>>()
                        {
                            @Nonnull
                            @Override
                            public List<String> load( @Nonnull Method key ) throws Exception
                            {
                                return Collections.unmodifiableList( asList( paranamer.lookupParameterNames( key, false ) ) );
                            }
                        } );

    @Nonnull
    public static List<String> getParameterNames( @Nonnull Method method )
    {
        try
        {
            return cache.getUnchecked( method );
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

    public static void clearCaches()
    {
        cache.invalidateAll();
    }

    private MethodParameterNames()
    {
    }
}
