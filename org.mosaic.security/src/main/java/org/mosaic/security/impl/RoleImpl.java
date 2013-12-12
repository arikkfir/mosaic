package org.mosaic.security.impl;

import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;
import org.mosaic.security.Permission;
import org.mosaic.util.xml.XmlElement;

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

    public RoleImpl( @Nonnull XmlElement element )
    {
        this.name = element.requireAttribute( "name" );
        for( XmlElement childRoleElement : element.getChildElements( "role" ) )
        {
            this.impliedRoles.add( new RoleImpl( childRoleElement ) );
        }
        for( XmlElement permissionElement : element.getChildElements( "permission" ) )
        {
            this.permissions.add( Permission.get( permissionElement.requireAttribute( "name" ) ) );
        }
    }

    @Nonnull
    public String getName()
    {
        return name;
    }

    public boolean implies( @Nonnull Permission permission )
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
