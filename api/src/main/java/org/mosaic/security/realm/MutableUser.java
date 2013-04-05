package org.mosaic.security.realm;

import javax.annotation.Nonnull;
import org.mosaic.security.Principal;
import org.mosaic.security.User;

/**
 * @author arik
 */
public interface MutableUser extends User
{
    @Nonnull
    MutableUser setName( @Nonnull String name );

    @Nonnull
    MutableUser addRole( @Nonnull String role );

    @Nonnull
    MutableUser addPrincipal( @Nonnull Principal principal );

    @Nonnull
    MutableUser addCredential( @Nonnull Object credential );
}
