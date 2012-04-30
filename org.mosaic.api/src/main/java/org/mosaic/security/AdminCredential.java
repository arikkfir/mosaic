package org.mosaic.security;

/**
 * @author arik
 */
public class AdminCredential implements User.Credential {

    @Override
    public String getName() {
        return "admin";
    }

    @Override
    public String getType() {
        return "admin";
    }
}
