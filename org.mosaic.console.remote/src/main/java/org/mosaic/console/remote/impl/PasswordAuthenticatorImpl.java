package org.mosaic.console.remote.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.sshd.server.session.ServerSession;
import org.mosaic.modules.Service;
import org.mosaic.security.AuthenticationException;
import org.mosaic.security.AuthenticationToken;
import org.mosaic.security.Security;
import org.mosaic.security.Subject;
import org.mosaic.security.support.UsernamePasswordAuthToken;

/**
 * @author arik
 */
final class PasswordAuthenticatorImpl implements org.apache.sshd.server.PasswordAuthenticator
{
    @Nonnull
    @Service
    private Security security;

    @Override
    public boolean authenticate( @Nonnull String username,
                                 @Nullable String password,
                                 @Nonnull ServerSession session )
    {
        if( username.trim().isEmpty() || password == null || password.trim().isEmpty() )
        {
            return false;
        }
        else
        {
            try
            {
                AuthenticationToken authenticationToken = new UsernamePasswordAuthToken( username, password.toCharArray(), false );
                Subject subject = this.security.authenticate( "localUsers", "shell", authenticationToken );
                session.setAttribute( SshServer.SUBJECT_KEY, subject );
                return true;
            }
            catch( AuthenticationException e )
            {
                return false;
            }
        }
    }
}
