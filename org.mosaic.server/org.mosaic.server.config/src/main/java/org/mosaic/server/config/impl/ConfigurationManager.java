package org.mosaic.server.config.impl;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Iterator;
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

    private final Map<String, ConfigurationImpl> configurations = new ConcurrentHashMap<>();

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

    @ServiceBind
    public void addListener( MethodEndpointInfo methodEndpointInfo ) {
        if( methodEndpointInfo.isOfType( ConfigListener.class ) ) {
            String configurationName = getValue( methodEndpointInfo.getType() ).toString();
            getConfiguration( configurationName ).addListener( methodEndpointInfo );
        }
    }

    @ServiceUnbind
    public void removeListener( MethodEndpointInfo methodEndpointInfo ) {
        if( methodEndpointInfo.isOfType( ConfigListener.class ) ) {
            String configurationName = getValue( methodEndpointInfo.getType() ).toString();
            getConfiguration( configurationName ).removeListener( methodEndpointInfo );
        }
    }

    @PostConstruct
    public void init() {
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
        LOG.info( "Stopped configuration manager" );
    }

    private synchronized void scan() {
        try( DirectoryStream<Path> stream = newDirectoryStream( this.mosaicHome.getEtc(), "*.properties" ) ) {
            for( Path path : stream ) {
                path = path.normalize().toAbsolutePath();
                getConfiguration( path.getFileName().toString() ).refresh();
            }

            Iterator<Map.Entry<String, ConfigurationImpl>> iterator = this.configurations.entrySet().iterator();
            while( iterator.hasNext() ) {
                Map.Entry<String, ConfigurationImpl> entry = iterator.next();
                ConfigurationImpl configuration = entry.getValue();
                if( !exists( configuration.getPath() ) && configuration.getListeners().isEmpty() ) {
                    iterator.remove();
                }
            }

        } catch( IOException e ) {
            LOG.error( "Could not watch directory '{}': {}", this.mosaicHome.getEtc(), e.getMessage(), e );
        }
    }

    private synchronized ConfigurationImpl getConfiguration( String configName ) {
        if( configName.toLowerCase().endsWith( ".properties" ) ) {
            configName = configName.substring( 0, configName.length() - ".properties".length() );
        }

        ConfigurationImpl configuration = this.configurations.get( configName );
        if( configuration == null ) {
            Path file = this.mosaicHome.getEtc().resolve( configName + ".properties" );
            configuration = new ConfigurationImpl( file, this.conversionService );
            this.configurations.put( configName, configuration );
        }
        return configuration;
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
