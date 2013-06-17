package org.mosaic.shell.impl.auth;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.mina.core.session.IoSession;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.util.Buffer;
import org.apache.sshd.server.UserAuth;
import org.apache.sshd.server.session.ServerSession;

/**
 * @author arik
 */
public class UserAuthLocalhostNone implements UserAuth
{
    public static class Factory implements NamedFactory<UserAuth>
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

    public Boolean auth( @Nonnull ServerSession session, @Nullable String username, @Nullable Buffer buffer )
    {
        IoSession ioSession = session.getIoSession();
        if( ioSession == null )
        {
            return false;
        }

        SocketAddress remoteAddress = ioSession.getRemoteAddress();
        if( remoteAddress instanceof InetSocketAddress )
        {
            InetSocketAddress inetAddress = ( InetSocketAddress ) remoteAddress;
            String hostAddress = inetAddress.getAddress().getHostAddress();
            return hostAddress.equals( "127.0.0.1" ) || hostAddress.equals( "0:0:0:0:0:0:0:1" ); // NOPMD - it's ok to hard-code localhost address
        }
        return false;
    }

}
