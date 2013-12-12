package org.mosaic.security.impl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import org.mosaic.security.Permission;
import org.mosaic.security.Subject;

/**
 * @author arik
 */
public class PermissionPolicyImpl
{
    @Nonnull
    private final Map<String, RoleImpl> roles = new HashMap<>();

    @Nonnull
    private final List<GrantRule> grants = new LinkedList<>();

    public void addRole( @Nonnull RoleImpl role )
    {
        this.roles.put( role.getName(), role );
    }

    public void addGrant( @Nonnull GrantRule grant )
    {
        this.grants.add( grant );
    }

    public boolean isPermitted( @Nonnull Subject user, @Nonnull Permission permission )
    {
        for( String roleName : user.getRoles() )
        {
            RoleImpl role = this.roles.get( roleName );
            if( role != null && role.implies( permission ) )
            {
                return true;
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
