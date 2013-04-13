package org.mosaic.security.credentials;

import java.io.IOException;
import java.security.PublicKey;
import javax.annotation.Nonnull;

/**
 * @author arik
 */
public interface PublicKeys extends Iterable<PublicKey>
{
    boolean authorizedFor( @Nonnull PublicKey key ) throws IOException;
}
