package org.mosaic.server.impl;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.annotation.Nonnull;
import org.mosaic.server.Server;
import org.mosaic.util.version.Version;
import org.osgi.framework.BundleContext;

/**
 * @author arik
 */
final class ServerImpl implements Server
{
    @Nonnull
    private final Version version;

    private final boolean developmentMode;

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

    ServerImpl( @Nonnull BundleContext bundleContext ) throws IOException
    {
        this.version = new Version( bundleContext.getProperty( "mosaic.version" ) );
        this.developmentMode = Boolean.parseBoolean( bundleContext.getProperty( "mosaic.devMode" ) );
        this.home = Paths.get( bundleContext.getProperty( "mosaic.home" ) );
        this.apps = Paths.get( bundleContext.getProperty( "mosaic.home.apps" ) );
        this.etc = Paths.get( bundleContext.getProperty( "mosaic.home.etc" ) );
        this.lib = Paths.get( bundleContext.getProperty( "mosaic.home.lib" ) );
        this.logs = Paths.get( bundleContext.getProperty( "mosaic.home.logs" ) );
        this.work = Paths.get( bundleContext.getProperty( "mosaic.home.work" ) );
    }

    @Nonnull
    @Override
    public Version getVersion()
    {
        return this.version;
    }

    @Override
    public boolean isDevelopmentMode()
    {
        return this.developmentMode;
    }

    @Nonnull
    @Override
    public Path getHome()
    {
        return this.home;
    }

    @Nonnull
    @Override
    public Path getAppsPath()
    {
        return this.apps;
    }

    @Nonnull
    @Override
    public Path getEtcPath()
    {
        return this.etc;
    }

    @Nonnull
    @Override
    public Path getLibPath()
    {
        return this.lib;
    }

    @Nonnull
    @Override
    public Path getLogsPath()
    {
        return this.logs;
    }

    @Nonnull
    @Override
    public Path getWorkPath()
    {
        return this.work;
    }
}
