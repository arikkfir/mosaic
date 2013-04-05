package org.mosaic.security.credentials;

import javax.annotation.Nonnull;

/**
 * @author arik
 */
public interface Password
{
    char[] getPassword();

    boolean check( @Nonnull char[] password );
}
