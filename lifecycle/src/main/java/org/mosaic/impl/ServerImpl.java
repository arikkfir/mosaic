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

/**
 * @author arik
 */
public class ServerImpl implements Server, DisposableBean
{
    private static final Logger LOG = LoggerFactory.getLogger( ServerImpl.class );

    @Nonnull
    private final Version version;

    @Nonnull
    private final Path home = Paths.get( System.getProperty( "mosaic.home" ) );

    @Nonnull
    private final Path apps = Paths.get( System.getProperty( "mosaic.home.apps" ) );

    @Nonnull
    private final Path etc = Paths.get( System.getProperty( "mosaic.home.etc" ) );

    @Nonnull
    private final Path lib = Paths.get( System.getProperty( "mosaic.home.lib" ) );

    @Nonnull
    private final Path logs = Paths.get( System.getProperty( "mosaic.home.logs" ) );

    @Nonnull
    private final Path work = Paths.get( System.getProperty( "mosaic.home.work" ) );

    @Nonnull
    private final BundleContext bundleContext;

    @Nullable
    private ServiceRegistration<Server> serviceRegistration;

    public ServerImpl( @Nonnull BundleContext bundleContext )
    {
        String version = System.getProperty( "mosaic.version" ).replace( '-', '.' );
        this.version = new Version( version );
        this.bundleContext = bundleContext;
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
