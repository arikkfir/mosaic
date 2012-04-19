package org.mosaic.server.web.application.impl;

import java.util.*;
import java.util.regex.Pattern;
import org.w3c.dom.Element;

/**
 * @author arik
 */
public class Role {

    private final String name;

    private final Collection<Role> childRoles = new LinkedList<>();

    private final Set<Pattern> permissions = new HashSet<>();

    public Role( Element element ) {
        this.name = element.getLocalName();

        // parse our permissions
        List<Element> permissionElts = DomUtils.getChildElements( element, "permission" );
        for( Element permissionElt : permissionElts ) {
            this.permissions.add( Pattern.compile( permissionElt.getTextContent().trim() ) );
        }

        // parse all other nodes - every element which is not in the permission elements list is a sub-role element
        List<Element> childElements = DomUtils.getChildElements( element );
        for( Element childElement : childElements ) {
            if( !permissionElts.contains( childElement ) ) {
                this.childRoles.add( new Role( childElement ) );
            }
        }
    }

    public boolean hasPermission( String permission ) {
        for( Pattern pattern : this.permissions ) {
            if( pattern.matcher( permission ).matches() ) {
                return true;
            }
        }

        for( Role childRole : this.childRoles ) {
            if( childRole.hasPermission( permission ) ) {
                return true;
            }
        }

        return false;
    }

    public void populateRoles( Map<String, Role> roles ) {
        roles.put( this.name, this );
        for( Role childRole : this.childRoles ) {
            childRole.populateRoles( roles );
        }
    }
}
