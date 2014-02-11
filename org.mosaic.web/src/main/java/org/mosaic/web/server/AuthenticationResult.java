package org.mosaic.web.server;

/**
 * @author arik
 */
public enum AuthenticationResult
{
    /**
     * Relevant credentials found, but are illegal. This does NOT signal bad credentials such as wrong password,
     * but rather that the credentials format was malformed (eg. illegal authentication header, key decryption
     * failed, etc).
     */
    INVALID_CREDENTIALS,

    /**
     * Relevant credentials found and they were legal, but the actual authentication failed - eg. incorrect password.
     */
    AUTHENTICATION_FAILED,

    /**
     * Authentication succeeded.
     */
    AUTHENTICATED
}
