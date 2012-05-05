package org.mosaic.server.config.impl;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.mosaic.Home;
import org.mosaic.lifecycle.ContextRef;
import org.mosaic.lifecycle.ServiceRef;
import org.mosaic.util.logging.Logger;
import org.mosaic.util.logging.LoggerFactory;
import org.osgi.framework.BundleContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import static java.nio.file.Files.exists;
import static java.nio.file.Files.newDirectoryStream;

/**
 * @author arik
 */
@Component
public class ConfigurationManager
{

    private static final Logger LOG = LoggerFactory.getLogger( ConfigurationManager.class );

    private static final long SCAN_INTERVAL = 1000;

    private final Map<Path, ConfigurationImpl> configurations = new ConcurrentHashMap<>();

    private Home home;

    private ConversionService conversionService;

    private ConfigurationManager.Scanner scanner;

    private BundleContext bundleContext;

    @ContextRef
    public void setBundleContext( BundleContext bundleContext )
    {
        this.bundleContext = bundleContext;
    }

    @ServiceRef
    public void setHome( Home home )
    {
        this.home = home;
    }

    @Autowired
    public void setConversionService( ConversionService conversionService )
    {
        this.conversionService = conversionService;
    }

    @PostConstruct
    public void init()
    {
        scan();

        this.scanner = new Scanner();
        Thread t = new Thread( scanner, "ConfigurationsWatcher" );
        t.setDaemon( true );
        t.start();
    }

    @PreDestroy
    public void destroy()
    {
        if( this.scanner != null )
        {
            this.scanner.stop = true;
        }
    }

    private synchronized void scan()
    {

        try( DirectoryStream<Path> stream = newDirectoryStream( this.home.getEtc(), "*.properties" ) )
        {

            for( Path configFile : stream )
            {
                ConfigurationImpl configuration = this.configurations.get( configFile );
                if( configuration == null )
                {
                    configuration = new ConfigurationImpl( this.bundleContext, configFile, this.conversionService );
                    this.configurations.put( configFile, configuration );
                }
                configuration.refresh();
            }

        }
        catch( IOException e )
        {
            LOG.error( "Could not search for configurations in '{}': {}", this.home.getEtc(), e.getMessage(), e );
        }

        this.configurations.values().iterator();
        Iterator<ConfigurationImpl> iterator = this.configurations.values().iterator();
        while( iterator.hasNext() )
        {
            ConfigurationImpl configuration = iterator.next();
            if( !exists( configuration.getPath() ) )
            {
                configuration.unregister();
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
