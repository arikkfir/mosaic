package org.mosaic.runner;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;

/**
 * @author arik
 */
public class ServerHome
{
    private final File home;

    private final File boot;

    private final File etc;

    private final File logs;

    private final File work;

    public ServerHome() throws IOException
    {
        this.home = findMosaicHome();
        FileUtils.forceMkdir( this.home );
        System.setProperty( "mosaic.home", this.home.toString() );

        this.boot = new File( this.home, "boot" );
        FileUtils.forceMkdir( this.boot );
        System.setProperty( "mosaic.home.boot", this.boot.toString() );

        this.etc = new File( this.home, "etc" );
        FileUtils.forceMkdir( this.etc );
        System.setProperty( "mosaic.home.etc", this.etc.toString() );

        this.logs = new File( this.home, "logs" );
        FileUtils.forceMkdir( this.logs );
        System.setProperty( "mosaic.home.logs", this.logs.toString() );

        this.work = new File( this.home, "work" );
        FileUtils.forceMkdir( this.work );
        System.setProperty( "mosaic.home.work", this.work.toString() );
    }

    public File getHome()
    {
        return this.home;
    }

    public File getBoot()
    {
        return boot;
    }

    public File getEtc()
    {
        return this.etc;
    }

    public File getWork()
    {
        return this.work;
    }

    private static File findMosaicHome()
    {
        String homePath = System.getProperty( "mosaicHome" );
        if( homePath != null )
        {
            File home = new File( homePath );
            if( home.isAbsolute() )
            {
                return home;
            }
            else
            {
                return new File( new File( System.getProperty( "user.dir" ) ), homePath );
            }
        }
        else
        {
            return new File( System.getProperty( "user.dir" ) );
        }
    }

    public File getLogs()
    {
        return this.logs;
    }
}
