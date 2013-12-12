package org.mosaic.console.remote.impl;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import javax.annotation.Nonnull;
import org.apache.mina.core.session.IoSession;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.util.Buffer;
import org.apache.sshd.server.UserAuth;
import org.apache.sshd.server.auth.UserAuthNone;
import org.apache.sshd.server.session.ServerSession;
import org.mosaic.modules.Service;
import org.mosaic.security.AuthenticationException;
import org.mosaic.security.AuthenticationToken;
import org.mosaic.security.Security;
import org.mosaic.security.Subject;
import org.mosaic.security.support.UsernameNoneAuthToken;

/**
 * @author arik
 */
final class UserAuthLocalhostNone extends UserAuthNone
{
    static final class Factory implements NamedFactory<UserAuth>
    {
        @Nonnull
        public String getName()
        {
            return "none";
        }

        @Nonnull
        public UserAuth create()
        {
            return new UserAuthLocalhostNone();
        }
    }

    @Nonnull
    @Service
    private Security security;

    private UserAuthLocalhostNone()
    {
    }

    @Override
    public Boolean auth( ServerSession session, String username, Buffer buffer )
    {
        IoSession ioSession = session.getIoSession();
        SocketAddress remoteAddress = ioSession.getRemoteAddress();
        if( remoteAddress instanceof InetSocketAddress )
        {
            InetSocketAddress inetSocketAddress = ( InetSocketAddress ) remoteAddress;
            if( Arrays.asList( "localhost", "127.0.0.1", "::1" ).contains( inetSocketAddress.getHostString() ) )
            {
                try
                {
                    AuthenticationToken authenticationToken = new UsernameNoneAuthToken( username, inetSocketAddress, false );
                    Subject subject = this.security.authenticate( "localUsers", "shell", authenticationToken );
                    session.setAttribute( SshServer.SUBJECT_KEY, subject );
                    return true;
                }
                catch( AuthenticationException ignore )
                {
                }
            }
        }
        return false;
    }
}
