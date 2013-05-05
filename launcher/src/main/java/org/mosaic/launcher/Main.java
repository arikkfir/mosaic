package org.mosaic.launcher;

import javax.annotation.Nonnull;
import org.mosaic.launcher.util.SystemError;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
public class Main
{
    public static void main( @Nonnull String[] args )
    {
        installLoggingUncaughtExceptionHandler();
        try
        {
            new MosaicBuilder( System.getProperties() ).create().start();
        }
        catch( Exception e )
        {
            // handle all errors here
            SystemError.handle( e );
            System.exit( 1 );
        }
    }

    private static void installLoggingUncaughtExceptionHandler()
    {
        Thread.setDefaultUncaughtExceptionHandler( new Thread.UncaughtExceptionHandler()
        {
            @Override
            public void uncaughtException( Thread t, Throwable e )
            {
                LoggerFactory.getLogger( Main.class ).error( e.getMessage(), e );
            }
        } );
    }
}
