package org.mosaic.security.policy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public interface PermissionPoliciesManager
{
    @Nonnull
    Permission parsePermission( @Nonnull String permission );

    @Nullable
    PermissionPolicy getPolicy( @Nonnull String name );
}
