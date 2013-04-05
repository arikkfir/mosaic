package org.mosaic.it.it01.moduleA;

import javax.annotation.Nonnull;
import org.mosaic.Server;
import org.mosaic.lifecycle.annotation.Bean;
import org.mosaic.lifecycle.annotation.ServiceRef;

/**
 * @author arik
 */
@Bean
public class It01
{
    private Server server;

    @ServiceRef
    public void setServer( @Nonnull Server server )
    {
        this.server = server;
    }
}
