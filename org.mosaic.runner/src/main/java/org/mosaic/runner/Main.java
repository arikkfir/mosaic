package org.mosaic.runner;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import java.io.File;

/**
 * @author arik
 */
public class Main
{
    public static void main( String[] args )
    {
        try
        {
            // setup Mosaic home directory and structure
            ServerHome home = new ServerHome( );

            // first thing we want to do is setup the logging framework
            setupLogging( home.getEtc( ) );

            // now we can start the server
            ExitCode ExitStatus = new Runner( home ).run( );

            // the exit code we return the OS shell wrapper has meaning - return it appropriately here
            System.exit( ExitStatus.getCode( ) );
        }
        catch( SystemExitException e )
        {
            e.printStackTrace( System.err );
            System.exit( e.getExitCode( ).getCode( ) );
        }
        catch( Exception e )
        {
            e.printStackTrace( System.err );
            System.exit( ExitCode.UNKNOWN_ERROR.getCode( ) );
        }
    }

    private static void setupLogging( File etcDir ) throws SystemExitException
    {
        File logbackConfigFile = new File( etcDir, "logback.xml" );
        if( logbackConfigFile.exists( ) )
        {
            LoggerContext lc = ( LoggerContext ) org.slf4j.LoggerFactory.getILoggerFactory( );
            try
            {
                JoranConfigurator configurator = new JoranConfigurator( );
                configurator.setContext( lc );
                lc.reset( );
                configurator.doConfigure( logbackConfigFile );
                StatusPrinter.printInCaseOfErrorsOrWarnings( lc );
            }
            catch( JoranException e )
            {
                throw new SystemExitException( "logging configuration error: " +
                                               e.getMessage( ), e, ExitCode.CONFIG_ERROR );
            }
        }
    }

}
