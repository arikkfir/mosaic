package org.mosaic.web.security.impl;

import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.annotation.Nonnull;
import org.eclipse.jetty.util.B64Code;
import org.mosaic.modules.Service;
import org.mosaic.security.AuthenticationException;
import org.mosaic.security.Security;
import org.mosaic.security.Subject;
import org.mosaic.security.support.UsernamePasswordAuthToken;
import org.mosaic.web.request.HttpStatus;
import org.mosaic.web.request.WebRequest;
import org.mosaic.web.security.Authenticator;

import static java.util.Arrays.asList;

/**
 * @author arik
 */
@Service
final class BasicAuthenticator implements Authenticator
{
    @Nonnull
    @Service
    private Security security;

    @Nonnull
    @Override
    public List<String> getAuthenticationMethods()
    {
        return asList( "basic" );
    }

    @Nonnull
    public AuthenticationAction authenticate( @Nonnull WebRequest request )
    {
        String credentials = request.getHeaders().getAuthorization();
        if( credentials == null )
        {
            return new AuthenticationAction( AuthenticationResult.NO_CREDENTIALS );
        }

        int space = credentials.indexOf( ' ' );
        if( space <= 0 || space == credentials.length() - 1 )
        {
            return new AuthenticationAction( AuthenticationResult.INVALID_CREDENTIALS );
        }

        String method = credentials.substring( 0, space );
        if( !"basic".equalsIgnoreCase( method ) )
        {
            return new AuthenticationAction( AuthenticationResult.NO_CREDENTIALS );
        }

        credentials = credentials.substring( space + 1 );
        credentials = B64Code.decode( credentials, StandardCharsets.ISO_8859_1 );
        int i = credentials.indexOf( ':' );
        if( i <= 0 )
        {
            return new AuthenticationAction( AuthenticationResult.INVALID_CREDENTIALS );
        }

        try
        {
            Subject subject = this.security.authenticate(
                    request.getApplication().getSecurity().getRealmName(),
                    request.getApplication().getSecurity().getPermissionPolicyName(),
                    new UsernamePasswordAuthToken( credentials.substring( 0, i ), credentials.substring( i + 1 ) )
            );
            return new AuthenticationAction( subject, AuthenticationResult.AUTHENTICATED );
        }
        catch( AuthenticationException e )
        {
            return new AuthenticationAction( AuthenticationResult.AUTHENTICATION_FAILED );
        }
    }

    @Override
    public void challange( @Nonnull WebRequest request )
    {
        String realmName = request.getApplication().getSecurity().getRealmName();
        request.getResponse().getHeaders().setWwwAuthenticate( "basic realm=\"" + realmName + "\"" );
        request.getResponse().setStatus( HttpStatus.UNAUTHORIZED );
    }
}
