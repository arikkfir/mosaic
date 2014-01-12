package org.mosaic.security.impl;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Properties;
import javax.annotation.Nonnull;
import org.mosaic.modules.Component;
import org.mosaic.modules.Service;
import org.mosaic.security.*;
import org.mosaic.security.support.PublicKeyAuthenticationToken;
import org.mosaic.security.support.UsernameNoneAuthToken;
import org.mosaic.security.support.UsernamePasswordAuthToken;
import org.mosaic.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.file.Files.*;

/**
 * @author arik
 */
@Component
final class LocalUsersRealm
{
    private static final Logger LOG = LoggerFactory.getLogger( LocalUsersRealm.class );

    @Service
    @Nonnull
    private Server server;

    @Realm("localUsers")
    public AuthenticationResult authenticateLocalUser( @Nonnull AuthenticationToken authenticationToken )
    {
        if( authenticationToken instanceof UsernamePasswordAuthToken )
        {
            return authenticateUsernameAndPassword( ( UsernamePasswordAuthToken ) authenticationToken );
        }
        else if( authenticationToken instanceof PublicKeyAuthenticationToken )
        {
            return authenticatePublicKey( ( PublicKeyAuthenticationToken ) authenticationToken );
        }
        else if( authenticationToken instanceof UsernameNoneAuthToken )
        {
            return authenticateNone( ( UsernameNoneAuthToken ) authenticationToken );
        }
        throw new IllegalArgumentException( "Unsupported authentication token: " + authenticationToken );
    }

    @Nonnull
    private AuthenticationResult authenticateUsernameAndPassword( @Nonnull UsernamePasswordAuthToken token )
    {
        Path passwdFile = getLocalSecurityHome().resolve( "passwd" );
        if( notExists( passwdFile ) || !isReadable( passwdFile ) )
        {
            throw new AuthenticationException( "bad credentials", token );
        }

        Properties properties = new Properties();
        try( Reader reader = newBufferedReader( passwdFile, Charset.forName( "UTF-8" ) ) )
        {
            properties.load( reader );
        }
        catch( Exception e )
        {
            LOG.warn( "Error reading passwords from '{}': {}", passwdFile, e.getMessage(), e );
            throw new AuthenticationException( "bad credentials", token );
        }

        String storedPassword = properties.getProperty( token.getUsername() );
        if( storedPassword == null || storedPassword.isEmpty() )
        {
            throw new AuthenticationException( "bad credentials", token );
        }

        byte[] attemptedPasswordBytes = token.getPassword();
        if( attemptedPasswordBytes == null )
        {
            throw new AuthenticationException( "bad credentials", token );
        }
        String attemptedPassword = new String( attemptedPasswordBytes, Charset.forName( "UTF-8" ) );

        if( !storedPassword.equals( attemptedPassword ) )
        {
            throw new AuthenticationException( "bad credentials", token );
        }

        return new AuthenticationResult( token.getUsername(),
                                         Collections.<String>emptySet(),
                                         Collections.<Principal>emptySet(),
                                         Collections.emptySet() );
    }

    @Nonnull
    private AuthenticationResult authenticatePublicKey( @Nonnull PublicKeyAuthenticationToken token )
    {
        Path keysFile = getLocalSecurityHome().resolve( token.getUsername() + ".keys" );
        if( notExists( keysFile ) || !isReadable( keysFile ) )
        {
            throw new AuthenticationException( "bad credentials", token );
        }

        try
        {
            String keys = new String( Files.readAllBytes( keysFile ), Charset.forName( "UTF-8" ) );
            AuthorizedKeys authorizedKeys = new AuthorizedKeys( keys.trim() );
            if( !authorizedKeys.authorizedFor( token.getPublicKey() ) )
            {
                throw new AuthenticationException( "bad credentials", token );
            }

            return new AuthenticationResult( token.getUsername(),
                                             Collections.<String>emptySet(),
                                             Collections.<Principal>emptySet(),
                                             Collections.emptySet() );
        }
        catch( IOException e )
        {
            LOG.warn( "Error reading SSH authorized keys for user '{}' from '{}': {}", token.getUsername(), keysFile, e.getMessage(), e );
            throw new AuthenticationException( "bad credentials", token );
        }
    }

    @Nonnull
    private AuthenticationResult authenticateNone( @Nonnull UsernameNoneAuthToken token )
    {
        return new AuthenticationResult( token.getUsername(),
                                         Collections.<String>emptySet(),
                                         Collections.<Principal>emptySet(),
                                         Collections.emptySet() );
    }

    @Nonnull
    private Path getLocalSecurityHome()
    {
        return this.server.getEtcPath().resolve( "security" ).resolve( "local" );
    }
}
