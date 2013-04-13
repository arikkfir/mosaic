package org.mosaic.security.credentials;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public interface Password
{
    @Nullable
    char[] getPassword();

    boolean check( @Nonnull char[] password );
}
