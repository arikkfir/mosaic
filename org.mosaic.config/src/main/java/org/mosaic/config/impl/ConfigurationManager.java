package org.mosaic.config.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.mosaic.core.Server;
import org.mosaic.core.components.Component;
import org.mosaic.core.components.Inject;
import org.mosaic.core.util.Nonnull;

/**
 * @author arik
 */
@Component
class ConfigurationManager
{
    @Nonnull
    private final ScheduledExecutorService executor;

    @Nonnull
    @Inject
    private Server server;

    ConfigurationManager()
    {
        this.executor = Executors.newSingleThreadScheduledExecutor();
        this.executor.scheduleWithFixedDelay( this::scan, 0, 1, TimeUnit.SECONDS );
    }

    private void scan()
    {
        // TODO arik: implement ConfigurationManager.scan([])
    }

    private class ConfigurationFile
    {
        @Nonnull
        private final Path file;

        @Nonnull
        private final List<ConfigurationProxy> proxies = new LinkedList<>();

        @Nonnull
        private Properties values = new Properties();

        private ConfigurationFile( @Nonnull Path file )
        {
            this.file = file;
        }
    }

    private class ConfigurationProxy implements InvocationHandler
    {
        @Override
        public Object invoke( Object proxy, Method method, Object[] args ) throws Throwable
        {
            // TODO arik: implement ConfigurationProxy.invoke([proxy, method, args])
            throw new UnsupportedOperationException();
        }
    }
}
