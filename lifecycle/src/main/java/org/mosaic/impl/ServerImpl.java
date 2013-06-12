package org.mosaic.impl;

import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.Server;
import org.mosaic.lifecycle.impl.util.ServiceUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author arik
 */
public class ServerImpl implements Server, InitializingBean, DisposableBean
{
    /**
     * The port on which the shutdown and restart requests are sent. If changing this value, remember to change it also
     * in MosaicInstance.
     */
    private static final int SHUTDOWN_PORT = 38631;

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
        try( Socket socket = new Socket( InetAddress.getLoopbackAddress(), SHUTDOWN_PORT ) )
        {
            OutputStreamWriter writer = new OutputStreamWriter( socket.getOutputStream(), "UTF-8" );
            writer.write( "SHUTDOWN" );
            writer.flush();
        }
        catch( Exception e )
        {
            throw new IllegalStateException( "Error shutting down Mosaic server: " + e.getMessage(), e );
        }
    }

    @Override
    public void restart()
    {
        try( Socket socket = new Socket( InetAddress.getLoopbackAddress(), SHUTDOWN_PORT ) )
        {
            OutputStreamWriter writer = new OutputStreamWriter( socket.getOutputStream(), "UTF-8" );
            writer.write( "RESTART" );
            writer.flush();
        }
        catch( Exception e )
        {
            throw new IllegalStateException( "Error shutting down Mosaic server: " + e.getMessage(), e );
        }
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
