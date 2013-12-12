package org.mosaic.security.support;

import java.security.PublicKey;
import javax.annotation.Nonnull;
import org.mosaic.security.AuthenticationToken;

/**
 * @author arik
 */
public class PublicKeyAuthenticationToken implements AuthenticationToken
{
    @Nonnull
    private final String username;

    @Nonnull
    private final PublicKey publicKey;

    private final boolean rememberMe;

    public PublicKeyAuthenticationToken( @Nonnull String username, @Nonnull PublicKey publicKey, boolean rememberMe )
    {
        this.username = username;
        this.publicKey = publicKey;
        this.rememberMe = rememberMe;
    }

    @Nonnull
    public String getUsername()
    {
        return this.username;
    }

    @Nonnull
    public PublicKey getPublicKey()
    {
        return this.publicKey;
    }

    @Override
    public boolean rememberMe()
    {
        return this.rememberMe;
    }
}
