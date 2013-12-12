package org.mosaic.security;

import javax.annotation.Nonnull;

/**
 * @author arik
 */
public interface Security
{
    @Nonnull
    Subject authenticate( @Nonnull String realmName,
                          @Nonnull String permissionPolicyName,
                          @Nonnull AuthenticationToken authenticationToken )
            throws AuthenticationException;

    @Nonnull
    Subject getSubject();
}
