package org.mosaic.lifecycle.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.core.MetricsRegistry;
import com.yammer.metrics.core.Timer;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.lifecycle.Module;

/**
 * @author arik
 */
public class MetricsImpl implements Module.Metrics
{
    @Nonnull
    private final Module module;

    @Nullable
    private MetricsRegistry metricsRegistry;

    @Nullable
    private LoadingCache<MetricName, MetricsTimerImpl> timerCache;

    public MetricsImpl( @Nonnull Module module )
    {
        this.module = module;
        this.metricsRegistry = new MetricsRegistry();
        this.timerCache = CacheBuilder.newBuilder()
                                      .initialCapacity( 1000 )
                                      .concurrencyLevel( 20 )
                                      .build( new CacheLoader<MetricName, MetricsTimerImpl>()
                                      {
                                          @Override
                                          public MetricsTimerImpl load( MetricName key ) throws Exception
                                          {
                                              Timer timer = metricsRegistry.newTimer( key, TimeUnit.MILLISECONDS, TimeUnit.SECONDS );
                                              return new MetricsTimerImpl( MetricsImpl.this.module, key, timer );
                                          }
                                      } );
    }

    @Nonnull
    @Override
    public Module.MetricsTimer getTimer( @Nonnull String group, @Nonnull String type, @Nonnull String name )
    {
        final MetricsRegistry metricsRegistry = this.metricsRegistry;
        LoadingCache<MetricName, MetricsTimerImpl> cache = this.timerCache;

        if( metricsRegistry != null && cache != null )
        {
            final MetricName key = new MetricName( group, type, name );
            try
            {
                return cache.get( key );
            }
            catch( ExecutionException e )
            {
                throw new IllegalStateException( "Could not create metrics timer for '" + group + ":" + type + ":" + name + "'", e );
            }
        }
        throw new IllegalStateException( "Could not create metrics timer for '" + group + ":" + type + ":" + name + "'" );
    }

    @Nonnull
    @Override
    public Collection<? extends Module.MetricsTimer> getTimers()
    {
        LoadingCache<MetricName, MetricsTimerImpl> cache = this.timerCache;
        if( cache != null )
        {
            return cache.asMap().values();
        }
        throw new IllegalStateException( "Metrics timers are not available" );
    }

    public void shutdown()
    {
        MetricsRegistry metricsRegistry = this.metricsRegistry;
        if( metricsRegistry != null )
        {
            metricsRegistry.shutdown();
        }
        this.metricsRegistry = null;

        LoadingCache<MetricName, MetricsTimerImpl> cache = this.timerCache;
        if( cache != null )
        {
            cache.invalidateAll();
        }
        this.timerCache = null;
    }
}
