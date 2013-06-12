package org.mosaic.lifecycle.impl;

import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;
import java.util.Deque;
import java.util.LinkedList;
import javax.annotation.Nonnull;
import org.mosaic.lifecycle.Module;

/**
 * @author arik
 */
public class MetricsTimerImpl implements Module.MetricsTimer
{
    @Nonnull
    private final ThreadLocal<Deque<TimerContext>> timerContexts = new ThreadLocal<Deque<TimerContext>>()
    {
        @Override
        protected Deque<TimerContext> initialValue()
        {
            return new LinkedList<>();
        }
    };

    @Nonnull
    private final Module module;

    @Nonnull
    private final MetricName name;

    @Nonnull
    private final Timer timer;

    public MetricsTimerImpl( @Nonnull Module module, @Nonnull MetricName name, @Nonnull Timer timer )
    {
        this.module = module;
        this.name = name;
        this.timer = timer;
    }

    @Nonnull
    @Override
    public Module getModule()
    {
        return this.module;
    }

    @Nonnull
    @Override
    public String getName()
    {
        return this.name.toString();
    }

    @Override
    public void startTimer()
    {
        this.timerContexts.get().push( this.timer.time() );
    }

    @Override
    public void stopTimer()
    {
        this.timerContexts.get().pop().stop();
    }

    @Override
    public long count()
    {
        return this.timer.count();
    }

    @Override
    public double fifteenMinuteRate()
    {
        return this.timer.fifteenMinuteRate();
    }

    @Override
    public double fiveMinuteRate()
    {
        return this.timer.fiveMinuteRate();
    }

    @Override
    public double meanRate()
    {
        return this.timer.meanRate();
    }

    @Override
    public double oneMinuteRate()
    {
        return this.timer.oneMinuteRate();
    }

    @Override
    public double max()
    {
        return this.timer.max();
    }

    @Override
    public double min()
    {
        return this.timer.min();
    }

    @Override
    public double mean()
    {
        return this.timer.mean();
    }

    @Override
    public double stdDev()
    {
        return this.timer.stdDev();
    }

    @Override
    public double sum()
    {
        return this.timer.sum();
    }
}
