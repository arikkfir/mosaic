package org.mosaic.impl;

import java.nio.file.Path;
import java.nio.file.Paths;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.Server;
import org.mosaic.lifecycle.impl.util.ServiceUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author arik
 */
public class ServerImpl implements Server, InitializingBean, DisposableBean
{
    private static final Logger LOG = LoggerFactory.getLogger( ServerImpl.class );

    @Nonnull
    private final Version version;

    @Nonnull
    private final Path home;

    @Nonnull
    private final Path apps;

    @Nonnull
    private final Path etc;

    @Nonnull
    private final Path lib;

    @Nonnull
    private final Path logs;

    @Nonnull
    private final Path work;

    @Nonnull
    private final BundleContext bundleContext;

    @Nullable
    private ServiceRegistration<Server> serviceRegistration;

    public ServerImpl( @Nonnull BundleContext bundleContext )
    {
        this.bundleContext = bundleContext;
        this.home = Paths.get( this.bundleContext.getProperty( "mosaic.home" ) );
        this.apps = Paths.get( this.bundleContext.getProperty( "mosaic.home.apps" ) );
        this.etc = Paths.get( this.bundleContext.getProperty( "mosaic.home.etc" ) );
        this.lib = Paths.get( this.bundleContext.getProperty( "mosaic.home.lib" ) );
        this.logs = Paths.get( this.bundleContext.getProperty( "mosaic.home.logs" ) );
        this.work = Paths.get( this.bundleContext.getProperty( "mosaic.home.work" ) );
        this.version = new Version( this.bundleContext.getProperty( "mosaic.version" ).replace( '-', '.' ) );
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        this.serviceRegistration = ServiceUtils.register( bundleContext, Server.class, this );
    }

    @Override
    public void destroy() throws Exception
    {
        this.serviceRegistration = ServiceUtils.unregister( this.serviceRegistration );
        this.serviceRegistration = null;
    }

    @Override
    public void shutdown()
    {
        new Thread( new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    // please ensure this logging dogma matches the one in launcher module's EventsLogger
                    LoggerFactory.getLogger( "org.osgi.framework" ).warn( "" );
                    LoggerFactory.getLogger( "org.osgi.framework" ).warn( "*****************************************************************************************" );
                    LoggerFactory.getLogger( "org.osgi.framework" ).warn( "Shutting down Mosaic server" );
                    LoggerFactory.getLogger( "org.osgi.framework" ).warn( "*****************************************************************************************" );
                    LoggerFactory.getLogger( "org.osgi.framework" ).warn( "" );

                    // stop the OSGi container (the mosaic launcher will take care of the rest)
                    bundleContext.getBundle( 0 ).stop();
                }
                catch( Exception e )
                {
                    LOG.error( "Could not shutdown Mosaic server: {}", e.getMessage(), e );
                }
            }
        }, "MosaicShutdown" ).start();
    }

    @Override
    public void restart()
    {
        // let the mosaic launcher know we want to restart, rather than stop
        System.setProperty( "mosaic.restarting", "true" );

        new Thread( new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    // please ensure this logging dogma matches the one in launcher module's EventsLogger
                    LoggerFactory.getLogger( "org.osgi.framework" ).warn( "" );
                    LoggerFactory.getLogger( "org.osgi.framework" ).warn( "*****************************************************************************************" );
                    LoggerFactory.getLogger( "org.osgi.framework" ).warn( "Restarting Mosaic server" );
                    LoggerFactory.getLogger( "org.osgi.framework" ).warn( "*****************************************************************************************" );
                    LoggerFactory.getLogger( "org.osgi.framework" ).warn( "" );

                    // stop the OSGi container (the mosaic launcher will take care of the rest)
                    bundleContext.getBundle( 0 ).stop();
                }
                catch( Exception e )
                {
                    LOG.error( "Could not restart Mosaic server: {}", e.getMessage(), e );
                }
            }
        }, "MosaicRestart" ).start();
    }

    @Nonnull
    @Override
    public String getVersion()
    {
        return this.version.toString();
    }

    @Nonnull
    @Override
    public Path getHome()
    {
        return this.home;
    }

    @Nonnull
    @Override
    public Path getApps()
    {
        return this.apps;
    }

    @Nonnull
    @Override
    public Path getEtc()
    {
        return this.etc;
    }

    @Nonnull
    @Override
    public Path getLib()
    {
        return this.lib;
    }

    @Nonnull
    @Override
    public Path getLogs()
    {
        return this.logs;
    }

    @Nonnull
    @Override
    public Path getWork()
    {
        return this.work;
    }
}
