package org.mosaic.it.modules.module01;

import java.io.IOException;
import javax.annotation.Nonnull;
import org.mosaic.Server;
import org.mosaic.lifecycle.annotation.Bean;
import org.mosaic.lifecycle.annotation.ServiceRef;

/**
 * @author arik
 */
@SuppressWarnings( { "FieldCanBeLocal", "UnusedDeclaration" } )
@Bean
public class BeanWithServerRef
{
    private Server server;

    @ServiceRef
    public void setServer( @Nonnull Server server ) throws IOException
    {
        this.server = server;
    }
}