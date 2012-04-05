package org.mosaic.runner;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.Files.*;

/**
 * @author arik
 */
public class MosaicHomeInternal {

    private final Path userDir = Paths.get( System.getProperty( "user.dir" ) ).normalize().toAbsolutePath();

    private final Path home = userDir.resolve( Paths.get( System.getProperty( "mosaicHome", "." ) ) ).normalize().toAbsolutePath();

    private final Path boot = home.resolve( "boot" );

    private final Path deploy = home.resolve( "deploy" );

    private final Path etc = home.resolve( "etc" );

    private final Path server = home.resolve( "server" );

    private final Path work = home.resolve( "work" );

    private final Path felixWork = work.resolve( "felix" );

    public MosaicHomeInternal() throws ConfigurationException, IOException {
        if( notExists( this.home ) ) {
            createDirectory( this.home );
        }
        System.setProperty( "mosaic.home", this.home.toString() );

        if( !exists( this.deploy ) ) {
            createDirectory( this.deploy );
        }
        System.setProperty( "mosaic.home.deploy", this.deploy.toString() );

        if( !exists( this.boot ) ) {
            createDirectory( this.boot );
        }
        System.setProperty( "mosaic.home.boot", this.boot.toString() );

        if( !exists( this.etc ) ) {
            createDirectory( this.etc );
        }
        System.setProperty( "mosaic.home.etc", this.etc.toString() );

        if( !exists( this.server ) ) {
            createDirectory( this.server );
        }
        System.setProperty( "mosaic.home.server", this.server.toString() );

        if( !exists( this.work ) ) {
            createDirectory( this.work );
        }
        System.setProperty( "mosaic.home.work", this.work.toString() );
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public Path getWorkDirectory() {
        return this.userDir;
    }

    public Path getHome() {
        return this.home;
    }

    public Path getBoot() {
        return boot;
    }

    public Path getDeploy() {
        return this.deploy;
    }

    public Path getEtc() {
        return this.etc;
    }

    public Path getServer() {
        return this.server;
    }

    public Path getWork() {
        return this.work;
    }

    public Path getFelixWork() {
        return this.felixWork;
    }
}
