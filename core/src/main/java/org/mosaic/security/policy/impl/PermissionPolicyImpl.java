package org.mosaic.security.policy.impl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import org.mosaic.security.User;
import org.mosaic.security.policy.PermissionPolicy;

/**
 * @author arik
 */
public class PermissionPolicyImpl implements PermissionPolicy
{
    @Nonnull
    private final Map<String, RoleImpl> roles = new HashMap<>();

    @Nonnull
    private final List<GrantRule> grants = new LinkedList<>();

    @SuppressWarnings("UnusedDeclaration")
    public void addRole( @Nonnull RoleImpl role )
    {
        this.roles.put( role.getName(), role );
    }

    @SuppressWarnings("UnusedDeclaration")
    public void addGrant( @Nonnull GrantRule grant )
    {
        this.grants.add( grant );
    }

    @Override
    public boolean isPermitted( @Nonnull User user, @Nonnull String permission )
    {
        for( String roleName : user.getRoles() )
        {
            RoleImpl role = this.roles.get( roleName );
            if( role != null )
            {
                if( role.implies( permission ) )
                {
                    return true;
                }
            }
        }

        for( GrantRule grant : this.grants )
        {
            if( grant.implies( user, permission ) )
            {
                return true;
            }
        }

        return false;
    }
}
