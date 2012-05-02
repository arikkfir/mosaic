package org.mosaic.security;

/**
 * @author arik
 */
public class RoleCredential implements User.Credential {

    private final String roleName;

    public RoleCredential( String roleName ) {
        this.roleName = roleName;
    }

    @Override
    public String getName( ) {
        return this.roleName;
    }

    @Override
    public String getType( ) {
        return "role";
    }

    @Override
    public String toString( ) {
        return "Role[" + this.roleName + "]";
    }
}
