package org.mosaic.server.config.impl;

import javax.annotation.PostConstruct;
import org.mosaic.MosaicHome;
import org.mosaic.lifecycle.ServiceRef;
import org.mosaic.logging.Logger;
import org.mosaic.logging.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author arik
 */
@Component
public class ConfigurationManager {

    private static final Logger LOG = LoggerFactory.getLogger( ConfigurationManager.class );

    private MosaicHome mosaicHome;

    @ServiceRef
    public void setMosaicHome( MosaicHome mosaicHome ) {
        this.mosaicHome = mosaicHome;
    }

    @PostConstruct
    public void init() {

    }
}
