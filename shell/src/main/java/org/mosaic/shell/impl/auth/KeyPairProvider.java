package org.mosaic.shell.impl.auth;

import javax.annotation.Nonnull;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.mosaic.Server;
import org.mosaic.lifecycle.annotation.Bean;
import org.mosaic.lifecycle.annotation.ServiceRef;

/**
 * @author arik
 */
@Bean
public class KeyPairProvider extends SimpleGeneratorHostKeyProvider
{
    @ServiceRef
    public void setServer( @Nonnull Server server )
    {
        setPath( server.getEtc().resolve( "host.key" ).toAbsolutePath().toString() );
    }
}
