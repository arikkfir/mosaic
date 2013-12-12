package org.mosaic.console.remote.impl;

import java.security.PublicKey;
import javax.annotation.Nonnull;
import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.mosaic.modules.Service;
import org.mosaic.security.AuthenticationException;
import org.mosaic.security.AuthenticationToken;
import org.mosaic.security.Security;
import org.mosaic.security.Subject;
import org.mosaic.security.support.PublicKeyAuthenticationToken;

/**
 * @author arik
 */
final class PublickeyAuthenticatorImpl implements PublickeyAuthenticator
{
    @Nonnull
    @Service
    private Security security;

    @Override
    public boolean authenticate( @Nonnull String username, @Nonnull PublicKey key, @Nonnull ServerSession session )
    {
        try
        {
            AuthenticationToken authenticationToken = new PublicKeyAuthenticationToken( username, key, false );
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
