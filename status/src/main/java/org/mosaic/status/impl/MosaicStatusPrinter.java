package org.mosaic.status.impl;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import org.mosaic.lifecycle.Module;
import org.mosaic.lifecycle.ModuleManager;
import org.mosaic.lifecycle.ModuleState;
import org.mosaic.lifecycle.annotation.Bean;
import org.mosaic.lifecycle.annotation.ServiceRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
@Bean
public class MosaicStatusPrinter
{
    private static final Logger LOG = LoggerFactory.getLogger( MosaicStatusPrinter.class );

    @Nonnull
    private ModuleManager moduleManager;

    @ServiceRef
    public void setModuleManager( @Nonnull ModuleManager moduleManager )
    {
        this.moduleManager = moduleManager;
    }

    @PostConstruct
    public void init()
    {
        for( Module module : this.moduleManager.getModules() )
        {
            if( module.getState() != ModuleState.ACTIVE )
            {
                LOG.warn( "Module {} could NOT be activated:", module );
                for( Module.Dependency dependency : module.getUnsatisfiedDependencies() )
                {
                    LOG.warn( "    -> {}", dependency );
                }
            }
        }
    }
}
