package org.mosaic.security;

/**
 * @author arik
 */
public interface PermissionPolicy
{
    boolean permits( String operation, User.Credential... credentials );
}
