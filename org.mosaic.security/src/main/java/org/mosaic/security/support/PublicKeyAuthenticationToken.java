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

    public PublicKeyAuthenticationToken( @Nonnull String username, @Nonnull PublicKey publicKey )
    {
        this.username = username;
        this.publicKey = publicKey;
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
}
