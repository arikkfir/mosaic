package org.mosaic.server.web.application.impl;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.mosaic.Home;
import org.mosaic.lifecycle.ServiceExport;
import org.mosaic.lifecycle.ServiceRef;
import org.mosaic.server.web.application.HttpApplicationManager;
import org.mosaic.util.logging.Logger;
import org.mosaic.util.logging.LoggerFactory;
import org.mosaic.web.HttpApplication;
import org.springframework.stereotype.Component;

import static java.nio.file.Files.exists;
import static java.nio.file.Files.newDirectoryStream;

/**
 * @author arik
 */
@Component
@ServiceExport( HttpApplicationManager.class )
public class HttpApplicationManagerImpl implements HttpApplicationManager
{
    private static final Logger LOG = LoggerFactory.getLogger( HttpApplicationManagerImpl.class );

    private static final long SCAN_INTERVAL = 1000;

    private final Map<Path, HttpApplicationImpl> applications = new HashMap<>();

    private Home home;

    private Scanner scanner;

    @ServiceRef
    public void setHome( Home home )
    {
        this.home = home;
    }

    @PostConstruct
    public synchronized void init()
    {
        scan();

        this.scanner = new Scanner();
        Thread t = new Thread( scanner, "ApplicationsWatcher" );
        t.setDaemon( true );
        t.start();
    }

    @PreDestroy
    public synchronized void destroy() throws SQLException
    {
        if( this.scanner != null )
        {
            this.scanner.stop = true;
        }
        this.applications.clear();
    }

    @Override
    public HttpApplication getApplication( String name )
    {
        for( HttpApplicationImpl application : this.applications.values() )
        {
            if( application.getName().equalsIgnoreCase( name ) )
            {
                return application;
            }
        }
        return null;
    }

    private synchronized void scan()
    {

        Path dir = this.home.getEtc().resolve( "apps" );
        if( Files.exists( dir ) && Files.isDirectory( dir ) )
        {

            try( DirectoryStream<Path> stream = newDirectoryStream( dir, "*.xml" ) )
            {

                for( Path appFile : stream )
                {
                    HttpApplicationImpl application = this.applications.get( appFile );
                    if( application == null )
                    {
                        application = new HttpApplicationImpl( appFile );
                        this.applications.put( appFile, application );
                    }
                    application.refresh();
                }

            }
            catch( IOException e )
            {
                LOG.error( "Could not search for applications in '{}': {}", dir, e.getMessage(), e );
            }

        }

        this.applications.values().iterator();
        Iterator<HttpApplicationImpl> iterator = this.applications.values().iterator();
        while( iterator.hasNext() )
        {
            HttpApplicationImpl application = iterator.next();
            if( !exists( application.getPath() ) )
            {
                iterator.remove();
            }
        }
    }

    private class Scanner implements Runnable
    {

        private boolean stop;

        @Override
        public void run()
        {
            while( !this.stop )
            {
                try
                {
                    Thread.sleep( SCAN_INTERVAL );
                }
                catch( InterruptedException e )
                {
                    break;
                }
                scan();
            }
        }
    }

}
