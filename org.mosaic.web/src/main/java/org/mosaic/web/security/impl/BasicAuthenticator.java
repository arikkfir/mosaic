package org.mosaic.web.security.impl;

import java.nio.charset.StandardCharsets;
import javax.annotation.Nonnull;
import org.eclipse.jetty.util.B64Code;
import org.mosaic.modules.Service;
import org.mosaic.security.AuthenticationException;
import org.mosaic.security.Security;
import org.mosaic.security.Subject;
import org.mosaic.security.support.UsernamePasswordAuthToken;
import org.mosaic.web.http.HttpStatus;
import org.mosaic.web.request.WebInvocation;
import org.mosaic.web.security.Authentication;
import org.mosaic.web.security.AuthenticationResult;
import org.mosaic.web.security.Authenticator;
import org.mosaic.web.security.Challanger;

/**
 * @author arik
 */
@Service(properties = @Service.P(key = "type", value = "basic"))
final class BasicAuthenticator implements Authenticator, Challanger
{
    @Nonnull
    @Service
    private Security security;

    @Nonnull
    @Override
    public String getAuthenticationMethod()
    {
        return "basic";
    }

    @Nonnull
    public Authentication authenticate( @Nonnull WebInvocation request )
    {
        String credentials = request.getHttpRequest().getAuthorization();
        if( credentials == null )
        {
            return new Authentication( AuthenticationResult.NO_CREDENTIALS );
        }

        int space = credentials.indexOf( ' ' );
        if( space <= 0 || space == credentials.length() - 1 )
        {
            return new Authentication( AuthenticationResult.INVALID_CREDENTIALS );
        }

        String method = credentials.substring( 0, space );
        if( !"basic".equalsIgnoreCase( method ) )
        {
            return new Authentication( AuthenticationResult.NO_CREDENTIALS );
        }

        credentials = credentials.substring( space + 1 );
        credentials = B64Code.decode( credentials, StandardCharsets.ISO_8859_1 );
        int i = credentials.indexOf( ':' );
        if( i <= 0 )
        {
            return new Authentication( AuthenticationResult.INVALID_CREDENTIALS );
        }

        try
        {
            Subject subject = this.security.authenticate(
                    request.getApplication().getSecurity().getRealmName(),
                    request.getApplication().getSecurity().getPermissionPolicyName(),
                    new UsernamePasswordAuthToken( credentials.substring( 0, i ), credentials.substring( i + 1 ) )
            );
            return new Authentication( subject, AuthenticationResult.AUTHENTICATED );
        }
        catch( AuthenticationException e )
        {
            return new Authentication( AuthenticationResult.AUTHENTICATION_FAILED );
        }
    }

    @Override
    public void challange( @Nonnull WebInvocation request )
    {
        String realmName = request.getApplication().getSecurity().getRealmName();
        request.getHttpResponse().setWwwAuthenticate( "basic realm=\"" + realmName + "\"" );
        request.getHttpResponse().setStatus( HttpStatus.UNAUTHORIZED, "Access denied" );
    }
}
