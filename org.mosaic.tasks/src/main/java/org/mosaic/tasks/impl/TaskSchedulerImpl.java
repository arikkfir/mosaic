package org.mosaic.tasks.impl;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PreDestroy;
import org.mosaic.config.Configurable;
import org.mosaic.modules.Component;
import org.mosaic.util.collections.EmptyMapEx;
import org.mosaic.util.collections.HashMapEx;
import org.mosaic.util.collections.MapEx;
import org.mosaic.util.scheduling.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
@Component
final class TaskSchedulerImpl
{
    private static final Logger LOG = LoggerFactory.getLogger( TaskSchedulerImpl.class );

    private static final Pattern TASK_PATTERN = Pattern.compile( "task\\.(\\p{Alnum}+)\\.?(.+)?" );

    @Nonnull
    private final ScheduledExecutorService taskExecutor;

    @SuppressWarnings( "MismatchedQueryAndUpdateOfCollection" )
    @Nonnull
    private final Map<String, TaskSchedule> schedules = new ConcurrentHashMap<>();

    @Nonnull
    @Component
    private TasksManagerImpl tasksManager;

    @Nullable
    private Timer timer;

    TaskSchedulerImpl()
    {
        this.taskExecutor = Executors.newScheduledThreadPool(
                10,
                new ThreadFactoryBuilder()
                        .setNameFormat( "Mosaic-TaskThread-%d" )
                        .setDaemon( true )
                        .build() );
    }

    @PreDestroy
    void destroy()
    {
        if( this.timer != null )
        {
            this.timer.cancel();
            this.timer = null;
        }

        this.taskExecutor.shutdown();
        try
        {
            this.taskExecutor.awaitTermination( 2, TimeUnit.MINUTES );
        }
        catch( InterruptedException e )
        {
            LOG.warn( "Timed out while waiting for @Task(s) to finish (tasks may still be running!)" );
        }
    }

    @Configurable( "jobs" )
    synchronized void updateJobSchedules( @Nonnull MapEx<String, String> cfg )
    {
        // kill old timer
        this.schedules.clear();
        if( this.timer != null )
        {
            this.timer.cancel();
            this.timer = null;
        }

        // create a new timer
        this.timer = new Timer( "Mosaic-Tasks-Timer", true );

        // collect task CRON expressions and task properties
        Map<String, CronExpression> cronExpressions = new LinkedHashMap<>();
        Map<String, MapEx<String, String>> properties = new HashMap<>();
        for( String key : cfg.keySet() )
        {
            Matcher matcher = TASK_PATTERN.matcher( key );
            if( matcher.matches() )
            {
                String taskName = matcher.group( 1 );

                if( matcher.groupCount() == 2 )
                {
                    MapEx<String, String> taskProperties = properties.get( taskName );
                    if( taskProperties == null )
                    {
                        taskProperties = new HashMapEx<>();
                        properties.put( taskName, taskProperties );
                    }
                    taskProperties.put( matcher.group( 2 ), cfg.get( key ) );
                }
                else
                {
                    try
                    {
                        cronExpressions.put( taskName, new CronExpression( cfg.get( key ) ) );
                    }
                    catch( Exception e )
                    {
                        LOG.warn( "CRON expression for task '{}' is illegal: {}", e.getMessage(), e );
                    }
                }
            }
        }

        // create schedules
        for( Map.Entry<String, CronExpression> entry : cronExpressions.entrySet() )
        {
            String taskName = entry.getKey();

            CronExpression cronExpression = cronExpressions.get( taskName );

            MapEx<String, String> taskProperties = properties.get( taskName );
            if( taskProperties == null )
            {
                taskProperties = EmptyMapEx.emptyMapEx();
            }

            TaskSchedule schedule = new TaskSchedule( taskName, cronExpression, taskProperties );
            this.schedules.put( taskName, schedule );
            schedule.scheduleNextInvocation();
        }
    }

    private class TaskSchedule
    {
        @Nonnull
        private final String taskName;

        @Nonnull
        private final CronExpression cronExpression;

        @Nonnull
        private final MapEx<String, String> properties;

        private TaskSchedule( @Nonnull String taskName,
                              @Nonnull CronExpression cronExpression,
                              @Nonnull MapEx<String, String> taskProperties )
        {
            this.taskName = taskName;
            this.cronExpression = cronExpression;
            this.properties = taskProperties;
        }

        private void scheduleNextInvocation()
        {
            Date nextSchedule = this.cronExpression.getNextValidTimeAfter( new Date() );
            if( nextSchedule != null )
            {
                Timer timer = TaskSchedulerImpl.this.timer;
                if( timer != null )
                {
                    try
                    {
                        timer.schedule( new TaskInvokerTask(), nextSchedule );
                    }
                    catch( IllegalStateException ignore )
                    {
                        // timer might close just after the nullity check and before we schedule; in this case, we
                        // simply ignore the error
                    }
                }
            }
        }

        /**
         * This timer task is run on the {@link #timer} and its job is simply to schedule an actual execution, on the
         * separate {@link #taskExecutor task executor}. This separation exists so that the timer thread is not "hogged"
         * by long running tasks.
         */
        private class TaskInvokerTask extends TimerTask
        {
            @Override
            public void run()
            {
                TaskSchedulerImpl.this.taskExecutor.submit( new TaskExecutor() );
            }
        }

        /**
         * This task is run on the {@link #taskExecutor} thread(s) and performs two actions: a) actually invoke the
         * task, and b) schedule the next invocation by scheduling a new {@link org.mosaic.tasks.impl.TaskSchedulerImpl.TaskSchedule.TaskInvokerTask timer task}
         * on the {@link #timer}.
         */
        private class TaskExecutor implements Runnable
        {
            @Override
            public void run()
            {
                // execute
                TaskAdapter adapter = TaskSchedulerImpl.this.tasksManager.findTask( TaskSchedule.this.taskName );
                if( adapter != null )
                {
                    adapter.execute( TaskSchedule.this.properties );
                }

                // schedule next invocation
                scheduleNextInvocation();
            }
        }
    }
}
