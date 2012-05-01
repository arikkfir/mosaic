/**
 * Copyright (c) 2011 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.mosaic.server.shell.impl.auth;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PublicKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPublicKey;
import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.mosaic.Home;
import org.mosaic.lifecycle.ServiceRef;
import org.mosaic.util.logging.Logger;
import org.mosaic.util.logging.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Copied from Eclipse git bundle to enable Mosaic SSH shell. Many thanks to the bright people, Gunnar in particular as
 * the author of this class, for providing this under the Eclipse license.
 *
 * @author Gunnar Wagenknecht
 */
@Component
public class AuthorizedPublicKeyAuthenticator implements PublickeyAuthenticator {

    private static final Logger LOG = LoggerFactory.getLogger( AuthorizedPublicKeyAuthenticator.class );

    private Home home;

    @ServiceRef
    public void setHome( Home home ) {
        this.home = home;
    }

    @Override
    public boolean authenticate( String username, PublicKey key, ServerSession session ) {

        Path authKeysFile = this.home.getEtc().resolve( "authorized_keys" );
        if( !Files.exists( authKeysFile ) || !Files.isReadable( authKeysFile ) ) {
            LOG.warn( "The '{}' file could not be found or read - no public key authentication can occur", authKeysFile );
            return false;
        }

        try {

            // dynamically read key file at each login attempt
            AuthorizedKeys keys = new AuthorizedKeys( authKeysFile );
            for( PublicKey authorizedKey : keys.getKeys() ) {
                if( isSameKey( authorizedKey, key ) ) {
                    return true;
                }
            }

        } catch( IOException e ) {
            LOG.warn( "Error reading authorized keys from '{}': {}", authKeysFile, e.getMessage(), e );
        }

        return false;
    }

    private boolean isSameKey( PublicKey k1, PublicKey k2 ) throws IOException {
        if( ( k1 instanceof DSAPublicKey ) && ( k2 instanceof DSAPublicKey ) ) {
            return isSameDSAKey( ( DSAPublicKey ) k1, ( DSAPublicKey ) k2 );
        } else if( ( k1 instanceof RSAPublicKey ) && ( k2 instanceof RSAPublicKey ) ) {
            return isSameRSAKey( ( RSAPublicKey ) k1, ( RSAPublicKey ) k2 );
        } else {
            throw new IOException( "Unsupported key types detected! (" + k1 + " and " + k2 + ")" );
        }
    }

    private boolean isSameRSAKey( RSAPublicKey k1, RSAPublicKey k2 ) {
        return k1.getPublicExponent().equals( k2.getPublicExponent() ) && k1.getModulus().equals( k2.getModulus() );
    }

    @SuppressWarnings( "SimplifiableIfStatement" )
    private boolean isSameDSAKey( DSAPublicKey k1, DSAPublicKey k2 ) {
        if( !k1.getY().equals( k2.getY() ) ) {
            return false;
        } else if( !k1.getParams().getG().equals( k2.getParams().getG() ) ) {
            return false;
        } else if( !k1.getParams().getP().equals( k2.getParams().getP() ) ) {
            return false;
        } else {
            return k1.getParams().getQ().equals( k2.getParams().getQ() );
        }
    }

}
