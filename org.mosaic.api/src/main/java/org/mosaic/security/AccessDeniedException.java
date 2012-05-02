package org.mosaic.security;

/**
 * @author arik
 */
public class AccessDeniedException extends Exception {

    public AccessDeniedException( ) {
        super( "Access denied" );
    }
}
