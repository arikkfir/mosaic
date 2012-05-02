package org.mosaic.server.boot.impl;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.mosaic.Home;

/**
 * @author arik
 */
public class HomeService implements Home
{

    private final Path home;

    private final Path boot;

    private final Path etc;

    private final Path work;

    public HomeService( )
    {
        this.home = Paths.get( System.getProperty( "mosaic.home" ) );
        this.boot = Paths.get( System.getProperty( "mosaic.home.boot" ) );
        this.etc = Paths.get( System.getProperty( "mosaic.home.etc" ) );
        this.work = Paths.get( System.getProperty( "mosaic.home.work" ) );
    }

    @Override
    public Path getHome( )
    {
        return this.home;
    }

    @Override
    public Path getBoot( )
    {
        return this.boot;
    }

    @Override
    public Path getEtc( )
    {
        return this.etc;
    }

    @Override
    public Path getWork( )
    {
        return this.work;
    }
}
