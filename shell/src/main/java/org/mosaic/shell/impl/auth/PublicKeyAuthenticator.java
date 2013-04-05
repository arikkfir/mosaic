package org.mosaic.shell.impl.auth;

import java.io.IOException;
import java.nio.file.Path;
import java.security.PublicKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPublicKey;
import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.mosaic.Server;
import org.mosaic.lifecycle.annotation.Bean;
import org.mosaic.lifecycle.annotation.ServiceRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.file.Files.*;

/**
 * @author arik
 */
@Bean
public class PublicKeyAuthenticator implements PublickeyAuthenticator
{
    private static final Logger LOG = LoggerFactory.getLogger( PublicKeyAuthenticator.class );

    private Server server;

    @ServiceRef
    public void setServer( Server server )
    {
        this.server = server;
    }

    @Override
    public boolean authenticate( String username, PublicKey key, ServerSession session )
    {
        Path etcDir = this.server.getEtc();
        if( notExists( etcDir ) || !isDirectory( etcDir ) )
        {
            return false;
        }

        Path authKeysDir = etcDir.resolve( "authorized_keys" );
        if( notExists( authKeysDir ) || !isDirectory( authKeysDir ) )
        {
            return false;
        }

        Path authKeysFile = authKeysDir.resolve( username );
        if( notExists( authKeysFile ) || !isReadable( authKeysFile ) )
        {
            return false;
        }

        try
        {
            // dynamically read key file at each login attempt
            AuthorizedKeys keys = new AuthorizedKeys( authKeysFile );
            for( PublicKey authorizedKey : keys.getKeys() )
            {
                if( isSameKey( authorizedKey, key ) )
                {
                    return true;
                }
            }
        }
        catch( IOException e )
        {
            LOG.warn( "Error reading authorized keys from '{}': {}", authKeysFile, e.getMessage(), e );
        }
        return false;
    }

    private boolean isSameKey( PublicKey k1, PublicKey k2 ) throws IOException
    {
        if( ( k1 instanceof DSAPublicKey ) && ( k2 instanceof DSAPublicKey ) )
        {
            return isSameDSAKey( ( DSAPublicKey ) k1, ( DSAPublicKey ) k2 );
        }
        else if( ( k1 instanceof RSAPublicKey ) && ( k2 instanceof RSAPublicKey ) )
        {
            return isSameRSAKey( ( RSAPublicKey ) k1, ( RSAPublicKey ) k2 );
        }
        else
        {
            throw new IOException( "Unsupported key types detected! (" + k1 + " and " + k2 + ")" );
        }
    }

    private boolean isSameRSAKey( RSAPublicKey k1, RSAPublicKey k2 )
    {
        return k1.getPublicExponent().equals( k2.getPublicExponent() ) && k1.getModulus().equals( k2.getModulus() );
    }

    @SuppressWarnings("SimplifiableIfStatement")
    private boolean isSameDSAKey( DSAPublicKey k1, DSAPublicKey k2 )
    {
        if( !k1.getY().equals( k2.getY() ) )
        {
            return false;
        }
        else if( !k1.getParams().getG().equals( k2.getParams().getG() ) )
        {
            return false;
        }
        else if( !k1.getParams().getP().equals( k2.getParams().getP() ) )
        {
            return false;
        }
        else
        {
            return k1.getParams().getQ().equals( k2.getParams().getQ() );
        }
    }
}
