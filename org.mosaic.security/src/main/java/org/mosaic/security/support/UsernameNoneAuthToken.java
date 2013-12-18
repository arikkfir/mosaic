package org.mosaic.security.support;

import java.net.InetSocketAddress;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.security.AuthenticationToken;

/**
 * @author arik
 */
public class UsernameNoneAuthToken implements AuthenticationToken
{
    @Nonnull
    private final String username;

    @Nullable
    private final InetSocketAddress address;

    public UsernameNoneAuthToken( @Nonnull String username, @Nullable InetSocketAddress address )
    {
        this.username = username;
        this.address = address;
    }

    @Nonnull
    public final String getUsername()
    {
        return this.username;
    }

    @Nullable
    public final InetSocketAddress getAddress()
    {
        return this.address;
    }
}
