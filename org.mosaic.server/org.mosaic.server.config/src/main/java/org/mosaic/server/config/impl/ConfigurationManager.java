package org.mosaic.server.config.impl;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.annotation.PostConstruct;
import org.mosaic.MosaicHome;
import org.mosaic.config.ConfigListener;
import org.mosaic.lifecycle.MethodEndpointInfo;
import org.mosaic.lifecycle.ServiceBind;
import org.mosaic.lifecycle.ServiceRef;
import org.mosaic.lifecycle.ServiceUnbind;
import org.mosaic.logging.Logger;
import org.mosaic.logging.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author arik
 */
@Component
public class ConfigurationManager {

    private static final Logger LOG = LoggerFactory.getBundleLogger( ConfigurationManager.class );

    private MosaicHome mosaicHome;

    private Set<MethodEndpointInfo> listeners = new CopyOnWriteArraySet<>();

    @ServiceRef
    public void setMosaicHome( MosaicHome mosaicHome ) {
        this.mosaicHome = mosaicHome;
    }

    @ServiceBind
    public void addListener( MethodEndpointInfo methodEndpointInfo ) {
        if( methodEndpointInfo.getType().equals( ConfigListener.class ) ) {
            LOG.info( "Added @ConfigListener '{}'", methodEndpointInfo );
            this.listeners.add( methodEndpointInfo );
        }
    }

    @ServiceUnbind
    public void removeListener( MethodEndpointInfo methodEndpointInfo ) {
        if( methodEndpointInfo.getType().equals( ConfigListener.class ) ) {
            this.listeners.remove( methodEndpointInfo );
            LOG.info( "Removed @ConfigListener '{}'", methodEndpointInfo );
        }
    }

    @PostConstruct
    public void init() {
        LOG.info( "Started configuration manager" );
    }
}
