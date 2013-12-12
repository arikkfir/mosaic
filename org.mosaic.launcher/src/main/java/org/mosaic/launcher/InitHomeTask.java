package org.mosaic.launcher;

import javax.annotation.Nonnull;

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.exists;
import static org.mosaic.launcher.IO.deletePath;
import static org.mosaic.launcher.SystemError.bootstrapError;

/**
 * @author arik
 */
final class InitHomeTask extends InitTask
{
    InitHomeTask( @Nonnull Mosaic mosaic )
    {
        super( mosaic );
    }

    @Override
    public void start()
    {
        this.log.debug( "Initializing home directory" );
        try
        {
            if( getConfiguration().isDevMode() )
            {
                if( exists( getConfiguration().getLogs() ) )
                {
                    this.log.debug( "Clearing logs directory (happens only in development mode)" );
                    deletePath( getConfiguration().getLogs() );
                }
            }

            this.log.debug( "Creating missing home directories" );
            createDirectories( getConfiguration().getHome() );
            createDirectories( getConfiguration().getApps() );
            createDirectories( getConfiguration().getBoot() );
            createDirectories( getConfiguration().getEtc() );
            createDirectories( getConfiguration().getLib() );
            createDirectories( getConfiguration().getLogs() );
            createDirectories( getConfiguration().getWork() );
        }
        catch( Exception e )
        {
            throw bootstrapError( "Could not initialize Mosaic home: {}", e.getMessage(), e );
        }
    }

    @Override
    public void stop()
    {
        // no-op
    }
}
