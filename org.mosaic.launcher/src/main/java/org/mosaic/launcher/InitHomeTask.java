package org.mosaic.launcher;

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.exists;

/**
 * @author arik
 */
final class InitHomeTask extends InitTask
{
    @Override
    public void start()
    {
        this.log.debug( "Initializing home directory" );
        try
        {
            if( Mosaic.isDevMode() )
            {
                if( exists( Mosaic.getLogs() ) )
                {
                    this.log.debug( "Clearing logs directory (happens only in development mode)" );
                    IO.deletePath( Mosaic.getLogs() );
                }
            }

            this.log.debug( "Creating missing home directories" );
            createDirectories( Mosaic.getHome() );
            createDirectories( Mosaic.getApps() );
            createDirectories( Mosaic.getEtc() );
            createDirectories( Mosaic.getLib() );
            createDirectories( Mosaic.getLogs() );
            createDirectories( Mosaic.getWork() );
        }
        catch( Exception e )
        {
            throw SystemError.bootstrapError( "Could not initialize Mosaic home: {}", e.getMessage(), e );
        }
    }

    @Override
    public void stop()
    {
        // no-op
    }
}
