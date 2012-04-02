package org.mosaic.runner;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.file.Files.*;

/**
 * @author arik
 */
public class MosaicHome {

    private final Path userDir = Paths.get( System.getProperty( "user.dir" ) ).normalize().toAbsolutePath();

    private final Path home = userDir.resolve( Paths.get( System.getProperty( "mosaicHome", "." ) ) ).normalize().toAbsolutePath();

    private final Path deploy = home.resolve( "deploy" );

    private final Path etc = home.resolve( "etc" );

    private final Path server = home.resolve( "server" );

    private final Path work = home.resolve( "work" );

    private final Path felixWork = work.resolve( "felix" );

    public MosaicHome() throws ConfigurationException, IOException {
        if( notExists( this.home ) ) {
            createDirectory( this.home );
        }
        if( !exists( this.deploy ) ) {
            createDirectory( this.deploy );
        }
        if( !exists( this.etc ) ) {
            createDirectory( this.etc );
        }
        if( !exists( this.server ) ) {
            createDirectory( this.server );
        }
        if( !exists( this.work ) ) {
            createDirectory( this.work );
        }

        Path logbackFile = etc.resolve( Paths.get( "logback.xml" ) );
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

        Logger logger = LoggerFactory.getLogger( MosaicHome.class );
        logger.info( "******************************************************************************************" );
        logger.info( "Starting Mosaic server" );
        logger.info( "    Home:           {}", this.home );
        logger.info( "    Deployments:    {}", this.deploy );
        logger.info( "    Configurations: {}", this.etc );
        logger.info( "    Server bundles: {}", this.server );
        logger.info( "    Work directory: {}", this.work );
        logger.info( "******************************************************************************************" );
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public Path getWorkDirectory() {
        return this.userDir;
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public Path getHome() {
        return this.home;
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public Path getDeploy() {
        return this.deploy;
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public Path getEtc() {
        return this.etc;
    }

    public Path getServer() {
        return this.server;
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public Path getWork() {
        return this.work;
    }

    public Path getFelixWork() {
        return this.felixWork;
    }
}
