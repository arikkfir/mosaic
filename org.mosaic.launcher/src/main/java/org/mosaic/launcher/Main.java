package org.mosaic.launcher;

import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
public final class Main
{
    private static final Logger LOG = LoggerFactory.getLogger( Main.class );

    public static void main( @Nonnull String[] args )
    {
        System.setProperty( "mosaic.launch.start", System.currentTimeMillis() + "" );

        // install an exception handler for all threads that don't have an exception handler, that simply logs the exception
        LOG.debug( "Installing default uncaught thread exception handler" );
        Thread.setDefaultUncaughtExceptionHandler( new Thread.UncaughtExceptionHandler()
        {
            @Override
            public void uncaughtException( Thread t, Throwable e )
            {
                LOG.error( e.getMessage(), e );
            }
        } );

        // start mosaic
        new Mosaic( new MosaicConfigurationBuilder().build() ).start();
    }
}
