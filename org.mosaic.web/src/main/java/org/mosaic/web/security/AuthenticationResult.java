package org.mosaic.web.security;

/**
 * @author arik
 */
public enum AuthenticationResult
{
    /**
     * No (or irrelevant) credentials provided, thus ignored by the authenticator.
     */
    NO_CREDENTIALS,

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
