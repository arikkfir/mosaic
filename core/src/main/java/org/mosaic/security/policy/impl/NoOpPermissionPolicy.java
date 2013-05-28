package org.mosaic.security.policy.impl;

import javax.annotation.Nonnull;
import org.mosaic.security.User;
import org.mosaic.security.policy.PermissionPolicy;

/**
 * @author arik
 */
public class NoOpPermissionPolicy implements PermissionPolicy
{
    public static final PermissionPolicy NO_OP_PERMISSION_POLICY = new NoOpPermissionPolicy();

    @Override
    public boolean isPermitted( @Nonnull User user, @Nonnull String permission )
    {
        return false;
    }
}
