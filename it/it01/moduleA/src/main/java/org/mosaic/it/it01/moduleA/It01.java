package org.mosaic.it.it01.moduleA;

import java.io.IOException;
import javax.annotation.Nonnull;
import org.mosaic.Server;
import org.mosaic.lifecycle.annotation.Bean;
import org.mosaic.lifecycle.annotation.ServiceRef;

import static java.nio.file.Files.write;
import static java.nio.file.StandardOpenOption.CREATE_NEW;

/**
 * @author arik
 */
@SuppressWarnings( { "FieldCanBeLocal", "UnusedDeclaration" } )
@Bean
public class It01
{
    private Server server;

    @ServiceRef
    public void setServer( @Nonnull Server server ) throws IOException
    {
        this.server = server;
        write( server.getWork().resolve( "it01.server.set" ), "".getBytes(), CREATE_NEW );
    }
}
