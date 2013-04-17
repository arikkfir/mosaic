package org.mosaic.security.policy.impl;

import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;
import org.mosaic.security.policy.Permission;

/**
 * @author arik
 */
public class RoleImpl
{
    @Nonnull
    private final String name;

    @Nonnull
    private final Set<Permission> permissions = new HashSet<>();

    @Nonnull
    private final Set<RoleImpl> impliedRoles = new HashSet<>();

    public RoleImpl( @Nonnull String name )
    {
        this.name = name;
    }

    @Nonnull
    public String getName()
    {
        return name;
    }

    public void addPermission( @Nonnull Permission permission )
    {
        this.permissions.add( permission );
    }

    @SuppressWarnings("UnusedDeclaration")
    public void addRole( @Nonnull RoleImpl role )
    {
        this.impliedRoles.add( role );
    }

    public boolean implies( @Nonnull String permission )
    {
        for( Permission grantedPermission : this.permissions )
        {
            if( grantedPermission.implies( permission ) )
            {
                return true;
            }
        }

        for( RoleImpl role : this.impliedRoles )
        {
            if( role.implies( permission ) )
            {
                return true;
            }
        }

        return false;
    }
}
