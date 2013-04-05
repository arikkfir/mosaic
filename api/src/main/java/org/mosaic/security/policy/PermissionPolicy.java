package org.mosaic.security.policy;

import javax.annotation.Nonnull;
import org.mosaic.security.User;

/**
 * @author arik
 */
public interface PermissionPolicy
{
    boolean isPermitted( @Nonnull User user, @Nonnull String permission );
}
