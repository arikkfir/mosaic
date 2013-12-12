package org.mosaic.launcher;

import java.nio.file.Path;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
abstract class InitTask implements MosaicConfiguration
{
    @Nonnull
    final Logger log = LoggerFactory.getLogger( getClass() );

    @Nonnull
    private final Mosaic mosaic;

    InitTask( @Nonnull Mosaic mosaic )
    {
        this.mosaic = mosaic;
    }

    public abstract void start();

    public abstract void stop();

    @Nonnull
    @Override
    public String getVersion()
    {
        return getConfiguration().getVersion();
    }

    @Override
    public boolean isDevMode()
    {
        return getConfiguration().isDevMode();
    }

    @Nonnull
    @Override
    public Path getHome()
    {
        return getConfiguration().getHome();
    }

    @Nonnull
    @Override
    public Path getBoot()
    {
        return getConfiguration().getBoot();
    }

    @Nonnull
    @Override
    public Path getApps()
    {
        return getConfiguration().getApps();
    }

    @Nonnull
    @Override
    public Path getEtc()
    {
        return getConfiguration().getEtc();
    }

    @Nonnull
    @Override
    public Path getLib()
    {
        return getConfiguration().getLib();
    }

    @Nonnull
    @Override
    public Path getLogs()
    {
        return getConfiguration().getLogs();
    }

    @Nonnull
    @Override
    public Path getWork()
    {
        return getConfiguration().getWork();
    }

    @Nonnull
    final Mosaic getMosaic()
    {
        return this.mosaic;
    }

    final MosaicConfiguration getConfiguration()
    {
        return this.mosaic.getConfiguration();
    }
}
