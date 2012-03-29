package org.mosaic.runner;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.mosaic.runner.exit.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
public class MosaicHome {

    private final Path userDir = Paths.get( System.getProperty( "user.dir" ) ).normalize().toAbsolutePath();

    private final Path home = userDir.resolve( Paths.get( System.getProperty( "mosaicHome", "." ) ) ).normalize().toAbsolutePath();

    private final Path bundles = home.resolve( "bundles" );

    private final Path etc = home.resolve( "etc" );

    private final Path server = home.resolve( "server" );

    private final Path work = home.resolve( "work" );

    private final Path felixWork = work.resolve( "felix" );

    public MosaicHome() throws ConfigurationException, IOException {
        if( Files.notExists( home ) ) {
            throw new ConfigurationException( "Mosaic home at '" + home + "' does not exist" );
        }
        if( !Files.exists( bundles ) ) {
            Files.createDirectory( bundles );
        }
        if( !Files.exists( etc ) ) {
            throw new ConfigurationException( "Mosaic 'etc' directory at '" + etc + "' does not exist" );
        }
        if( !Files.exists( server ) ) {
            throw new ConfigurationException( "Mosaic 'server' directory at '" + server + "' does not exist" );
        }
        if( !Files.exists( work ) ) {
            Files.createDirectory( work );
        }

        Path logbackFile = etc.resolve( Paths.get( "logback.xml" ) );
        if( Files.notExists( logbackFile ) ) {
            throw new ConfigurationException( "Could not find 'logback.xml' file at: " + logbackFile );
        }

        LoggerContext lc = ( LoggerContext ) LoggerFactory.getILoggerFactory();
        try {
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext( lc );
            lc.reset();
            configurator.doConfigure( logbackFile.toFile() );
            StatusPrinter.printInCaseOfErrorsOrWarnings( lc );

            Logger logger = LoggerFactory.getLogger( MosaicHome.class );
            logger.info( "******************************************************************************************" );
            logger.info( "Starting Mosaic server" );
            logger.info( "    Home:           {}", home );
            logger.info( "    Deployments:    {}", bundles );
            logger.info( "    Configurations: {}", etc );
            logger.info( "    Server bundles: {}", server );
            logger.info( "    Work directory: {}", work );
            logger.info( "******************************************************************************************" );

        } catch( JoranException e ) {
            throw new ConfigurationException( "logging configuration error: " + e.getMessage(), e );
        }
    }

    public Path getWorkDirectory() {
        return userDir;
    }

    public Path getHome() {
        return home;
    }

    public Path getBundles() {
        return bundles;
    }

    public Path getEtc() {
        return etc;
    }

    public Path getServer() {
        return server;
    }

    public Path getWork() {
        return work;
    }

    public Path getFelixWork() {
        return felixWork;
    }
}
