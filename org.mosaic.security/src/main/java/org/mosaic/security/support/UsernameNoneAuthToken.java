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

    private final boolean rememberMe;

    public UsernameNoneAuthToken( @Nonnull String username, @Nullable InetSocketAddress address, boolean rememberMe )
    {
        this.rememberMe = rememberMe;
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

    @Override
    public final boolean rememberMe()
    {
        return this.rememberMe;
    }
}
