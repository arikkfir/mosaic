package org.mosaic.server.boot.impl;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.mosaic.MosaicHome;

/**
 * @author arik
 */
public class MosaicHomeImpl implements MosaicHome {

    private final Path home;

    private final Path boot;

    private final Path deploy;

    private final Path etc;

    private final Path server;

    private final Path work;

    public MosaicHomeImpl() {
        this.home = Paths.get( System.getProperty( "mosaic.home" ) );
        this.boot = Paths.get( System.getProperty( "mosaic.boot" ) );
        this.deploy = Paths.get( System.getProperty( "mosaic.deploy" ) );
        this.etc = Paths.get( System.getProperty( "mosaic.etc" ) );
        this.server = Paths.get( System.getProperty( "mosaic.server" ) );
        this.work = Paths.get( System.getProperty( "mosaic.work" ) );
    }

    @Override
    public Path getHome() {
        return this.home;
    }

    @Override
    public Path getBoot() {
        return this.boot;
    }

    @Override
    public Path getDeploy() {
        return this.deploy;
    }

    @Override
    public Path getEtc() {
        return this.etc;
    }

    @Override
    public Path getServer() {
        return this.server;
    }

    @Override
    public Path getWork() {
        return this.work;
    }
}
