package org.mosaic.security.support;

import java.io.UnsupportedEncodingException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.security.AuthenticationToken;

/**
 * @author arik
 */
public class UsernamePasswordAuthToken implements AuthenticationToken
{
    private static byte[] getPasswordBytes( String password )
    {
        try
        {
            return password != null ? password.getBytes( "UTF-8" ) : null;
        }
        catch( UnsupportedEncodingException e )
        {
            throw new IllegalStateException( e.getMessage(), e );
        }
    }

    @Nonnull
    private final String username;

    @Nullable
    private final byte[] password;

    private final boolean rememberMe;

    public UsernamePasswordAuthToken( @Nonnull String username, @Nullable char[] password, boolean rememberMe )
    {
        this( username, password != null ? new String( password ) : null, rememberMe );
    }

    public UsernamePasswordAuthToken( @Nonnull String username, @Nullable String password, boolean rememberMe )
    {
        this( username, getPasswordBytes( password ), rememberMe );
    }

    public UsernamePasswordAuthToken( @Nonnull String username, @Nullable byte[] password, boolean rememberMe )
    {
        this.rememberMe = rememberMe;
        this.username = username;
        this.password = password;
    }

    @Nonnull
    public final String getUsername()
    {
        return this.username;
    }

    @Nullable
    public final byte[] getPassword()
    {
        return this.password;
    }

    @Override
    public final boolean rememberMe()
    {
        return this.rememberMe;
    }
}
