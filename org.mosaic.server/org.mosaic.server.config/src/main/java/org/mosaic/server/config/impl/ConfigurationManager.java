package org.mosaic.server.config.impl;

import org.mosaic.logging.Logger;
import org.mosaic.logging.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author arik
 */
@Component
public class ConfigurationManager {

    private static final Logger LOG = LoggerFactory.getLogger( ConfigurationManager.class );

    public ConfigurationManager() {
        LOG.info( "Configuration manager started" );
    }
}
