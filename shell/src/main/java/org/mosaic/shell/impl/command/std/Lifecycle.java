package org.mosaic.shell.impl.command.std;

import javax.annotation.Nonnull;
import org.mosaic.Server;
import org.mosaic.lifecycle.annotation.Bean;
import org.mosaic.lifecycle.annotation.ServiceRef;
import org.mosaic.shell.annotation.Command;

/**
 * @author arik
 */
@Bean
public class Lifecycle
{
    private Server server;

    @ServiceRef
    public void setServer( @Nonnull Server server )
    {
        this.server = server;
    }

    @Command(name = "restart-server", label = "Restart Mosaic", desc = "Restarts the Mosaic server.")
    public void restartServer()
    {
        server.restart();
    }

    @Command(name = "shutdown-server", label = "Shutdown Mosaic", desc = "Shuts down the Mosaic server.")
    public void shutdownServer()
    {
        server.shutdown();
    }
}
