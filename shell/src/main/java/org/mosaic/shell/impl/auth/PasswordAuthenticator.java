package org.mosaic.shell.impl.auth;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.sshd.server.session.ServerSession;
import org.mosaic.lifecycle.annotation.Bean;
import org.mosaic.lifecycle.annotation.Configurable;

/**
 * @author arik
 */
@Bean
public class PasswordAuthenticator implements org.apache.sshd.server.PasswordAuthenticator
{
    private Map<String, String> shellUsers = Collections.emptyMap();

    @Configurable( "users" )
    public void setShellUsers( @Nonnull Map<String, String> shellUsers )
    {
        this.shellUsers = new HashMap<>( shellUsers );
        if( this.shellUsers.isEmpty() )
        {
            // for initial installs, admins can change it later
            this.shellUsers.put( "admin", DigestUtils.md5Hex( "password" ) );
        }
    }

    @Override
    public boolean authenticate( @Nonnull String username, @Nullable String password, @Nonnull ServerSession session )
    {
        String correctPassword = this.shellUsers.get( username.toLowerCase() );
        if( correctPassword == null || correctPassword.trim().isEmpty() )
        {
            return false;
        }

        String sentPasswordMd5 = DigestUtils.md5Hex( password );
        return sentPasswordMd5.equals( correctPassword );
    }
}
