package org.mosaic.util.resource.impl;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.resource.PathMatcher;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * @author arik
 */
public class Activator implements BundleActivator
{
    @Nullable
    private ServiceRegistration<PathMatcher> registration;

    @Nullable
    private Scanner scanner;

    @Nullable
    private ScheduledExecutorService executorService;

    @Override
    public void start( @Nonnull BundleContext context ) throws Exception
    {
        PathMatcherImpl pathMatcher = new PathMatcherImpl();
        this.registration = context.registerService( PathMatcher.class, pathMatcher, null );

        this.scanner = new Scanner( context, pathMatcher );
        this.scanner.open();

        this.executorService = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder()
                        .setDaemon( true )
                        .setNameFormat( "PathScanner-%d" ).build() );
        this.executorService.scheduleWithFixedDelay( new Runnable()
        {
            @Override
            public void run()
            {
                Scanner scanner = Activator.this.scanner;
                if( scanner != null )
                {
                    scanner.scan();
                }
            }
        }, 0, 1, TimeUnit.SECONDS );

    }

    @Override
    public void stop( @Nonnull BundleContext context ) throws Exception
    {
        ScheduledExecutorService executorService = this.executorService;
        if( executorService != null )
        {
            executorService.shutdown();
            executorService.awaitTermination( 30, TimeUnit.SECONDS );
            this.executorService = null;
        }

        Scanner scanner = this.scanner;
        if( scanner != null )
        {
            scanner.close();
            this.scanner = null;
        }

        ServiceRegistration<PathMatcher> registration = this.registration;
        if( registration != null )
        {
            try
            {
                registration.unregister();
            }
            catch( Exception ignore )
            {
            }
            this.registration = null;
        }
    }
}
