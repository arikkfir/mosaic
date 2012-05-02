package org.mosaic.server.web.application.impl;

import java.util.*;
import org.w3c.dom.Element;

/**
 * @author arik
 */
public class Role
{

    private final String name;

    private final Collection<Role> childRoles = new LinkedList<>( );

    private final Set<String> permissions = new HashSet<>( );

    public Role( Element element )
    {
        this.name = element.getLocalName( );

        // parse our permissions
        List<Element> permissionElts = DomUtils.getChildElements( element, "permission" );
        for( Element permissionElt : permissionElts )
        {
            this.permissions.add( permissionElt.getTextContent( ).trim( ) );
        }

        // parse all other nodes - every element which is not in the permission elements list is a sub-role element
        List<Element> childElements = DomUtils.getChildElements( element );
        for( Element childElement : childElements )
        {
            if( !permissionElts.contains( childElement ) )
            {
                this.childRoles.add( new Role( childElement ) );
            }
        }
    }

    public String getName( )
    {
        return name;
    }

    public Set<String> getPermissionPatterns( )
    {
        Set<String> patterns = new HashSet<>( this.permissions );
        for( Role childRole : this.childRoles )
        {
            patterns.addAll( childRole.getPermissionPatterns( ) );
        }
        return patterns;
    }

    public void populateRoles( Map<String, Role> roles )
    {
        roles.put( this.name, this );
        for( Role childRole : this.childRoles )
        {
            childRole.populateRoles( roles );
        }
    }
}
