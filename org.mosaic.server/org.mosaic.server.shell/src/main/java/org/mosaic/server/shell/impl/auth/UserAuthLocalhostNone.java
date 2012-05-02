package org.mosaic.server.shell.impl.auth;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
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

        public String getName( )
        {
            return "none";
        }

        public UserAuth create( )
        {
            return new UserAuthLocalhostNone( );
        }
    }

    public Boolean auth( ServerSession session, String username, Buffer buffer )
    {
        IoSession ioSession = session.getIoSession( );
        if( ioSession == null )
        {
            return false;
        }

        SocketAddress remoteAddress = ioSession.getRemoteAddress( );
        if( remoteAddress instanceof InetSocketAddress )
        {
            InetSocketAddress inetAddress = ( InetSocketAddress ) remoteAddress;
            return inetAddress.getAddress( ).getHostAddress( ).equals( "127.0.0.1" );
        }
        return false;
    }

}
