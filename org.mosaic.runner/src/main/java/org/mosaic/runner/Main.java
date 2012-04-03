package org.mosaic.runner;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.Files.exists;

/**
 * @author arik
 */
public class Main {

    public static void main( String[] args ) {
        try {

            // setup Mosaic home directory and structure
            MosaicHome home = new MosaicHome();

            // first thing we want to do is setup the logging framework
            setupLogging( home );

            // now we can start the server
            ExitCode ExitStatus = new Runner( home ).run();

            // the exit code we return the OS shell wrapper has meaning - return it appropriately here
            System.exit( ExitStatus.getCode() );

        } catch( SystemExitException e ) {

            e.printStackTrace( System.err );
            System.exit( e.getExitCode().getCode() );

        } catch( Exception e ) {

            e.printStackTrace( System.err );
            System.exit( ExitCode.UNKNOWN_ERROR.getCode() );
        }
    }

    private static void setupLogging( MosaicHome home ) throws ConfigurationException {
        Path logbackFile = home.getEtc().resolve( Paths.get( "logback.xml" ) );
        if( exists( logbackFile ) ) {
            LoggerContext lc = ( LoggerContext ) org.slf4j.LoggerFactory.getILoggerFactory();
            try {
                JoranConfigurator configurator = new JoranConfigurator();
                configurator.setContext( lc );
                lc.reset();
                configurator.doConfigure( logbackFile.toFile() );
                StatusPrinter.printInCaseOfErrorsOrWarnings( lc );
            } catch( JoranException e ) {
                throw new ConfigurationException( "logging configuration error: " + e.getMessage(), e );
            }
        }
    }
}
