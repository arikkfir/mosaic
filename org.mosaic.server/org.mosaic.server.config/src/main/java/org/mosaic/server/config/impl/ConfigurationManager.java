package org.mosaic.server.config.impl;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.mosaic.MosaicHome;
import org.mosaic.config.ConfigListener;
import org.mosaic.lifecycle.MethodEndpointInfo;
import org.mosaic.lifecycle.ServiceBind;
import org.mosaic.lifecycle.ServiceRef;
import org.mosaic.lifecycle.ServiceUnbind;
import org.mosaic.logging.Logger;
import org.mosaic.logging.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import static java.nio.file.Files.exists;
import static java.nio.file.Files.newDirectoryStream;
import static org.springframework.core.annotation.AnnotationUtils.getValue;

/**
 * @author arik
 */
@Component
public class ConfigurationManager {

    private static final Logger LOG = LoggerFactory.getBundleLogger( ConfigurationManager.class );

    private static final long SCAN_INTERVAL = 1000;

    private final Map<Path, ConfigurationImpl> configurations = new ConcurrentHashMap<>();

    private final Collection<MethodEndpointInfo> listeners = new LinkedList<>();

    private ConversionService conversionService;

    private MosaicHome mosaicHome;

    private ConfigurationManager.Scanner scanner;

    @Autowired
    public void setConversionService( ConversionService conversionService ) {
        this.conversionService = conversionService;
    }

    @ServiceRef
    public void setMosaicHome( MosaicHome mosaicHome ) {
        this.mosaicHome = mosaicHome;
    }

    @ServiceBind( filter = "methodEndpointShortType=ConfigListener" )
    public synchronized void addListener( MethodEndpointInfo methodEndpointInfo ) {
        if( methodEndpointInfo.isOfType( ConfigListener.class ) ) {
            this.listeners.add( methodEndpointInfo );
            invokeListener( methodEndpointInfo, true );
        }
    }

    @ServiceUnbind( filter = "methodEndpointShortType=ConfigListener" )
    public synchronized void removeListener( MethodEndpointInfo methodEndpointInfo ) {
        if( methodEndpointInfo.isOfType( ConfigListener.class ) ) {
            this.listeners.remove( methodEndpointInfo );
        }
    }

    @PostConstruct
    public void init() {
        scan();

        this.scanner = new Scanner();
        Thread t = new Thread( scanner, "ConfigurationsWatcher" );
        t.setDaemon( true );
        t.start();
    }

    @PreDestroy
    public void destroy() {
        if( this.scanner != null ) {
            this.scanner.stop = true;
        }
    }

    private synchronized void invokeListener( MethodEndpointInfo listener, boolean invokeIfUpToDate ) {
        for( ConfigurationImpl configuration : this.configurations.values() ) {
            if( listenerMatchesConfiguration( listener, configuration ) ) {
                boolean updated = configuration.refresh();
                if( invokeIfUpToDate || updated ) {
                    configuration.invoke( listener );
                }
            }
        }
    }

    private synchronized void scan() {

        Collection<ConfigurationImpl> updated = new LinkedList<>();
        try( DirectoryStream<Path> stream = newDirectoryStream( this.mosaicHome.getEtc(), "*.properties" ) ) {

            for( Path configFile : stream ) {
                ConfigurationImpl configuration = this.configurations.get( configFile );
                if( configuration == null ) {
                    configuration = new ConfigurationImpl( configFile, this.conversionService );
                    this.configurations.put( configFile, configuration );
                }
                if( configuration.refresh() ) {
                    updated.add( configuration );
                }
            }

        } catch( IOException e ) {
            LOG.error( "Could not search for configurations in '{}': {}", this.mosaicHome.getEtc(), e.getMessage(), e );
        }

        for( ConfigurationImpl configuration : updated ) {
            for( MethodEndpointInfo listener : this.listeners ) {
                if( listenerMatchesConfiguration( listener, configuration ) ) {
                    configuration.invoke( listener );
                }
            }
        }

        this.configurations.values().iterator();
        Iterator<ConfigurationImpl> iterator = this.configurations.values().iterator();
        while( iterator.hasNext() ) {
            ConfigurationImpl configuration = iterator.next();
            if( !exists( configuration.getPath() ) ) {
                iterator.remove();
            }
        }
    }

    private boolean listenerMatchesConfiguration( MethodEndpointInfo listener, ConfigurationImpl configuration ) {
        String pattern = getValue( listener.getType() ).toString();
        return configuration.matches( "glob:" + this.mosaicHome.getEtc() + "/" + pattern + ".properties" );
    }

    private class Scanner implements Runnable {

        private boolean stop;

        @Override
        public void run() {
            while( !this.stop ) {
                try {
                    Thread.sleep( SCAN_INTERVAL );
                } catch( InterruptedException e ) {
                    break;
                }
                scan();
            }
        }
    }
}
