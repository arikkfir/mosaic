package org.mosaic.web.server.impl;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import javax.annotation.Nonnull;
import org.apache.commons.codec.binary.Base64;
import org.mosaic.modules.Service;
import org.mosaic.security.AuthenticationException;
import org.mosaic.security.Security;
import org.mosaic.security.Subject;
import org.mosaic.security.support.UsernamePasswordAuthToken;
import org.mosaic.web.server.*;

/**
 * @author arik
 */
@Service( properties = @Service.P( key = "type", value = "basic" ) )
final class BasicAuthenticator implements Authenticator, Challanger
{
    @Nonnull
    private final Base64 base64 = new Base64();

    @Nonnull
    @Service
    private Security security;

    @Nonnull
    public Optional<Authentication> authenticate( @Nonnull WebInvocation invocation )
    {
        String credentials = invocation.getHttpRequest().getAuthorization();
        if( credentials == null )
        {
            return Optional.absent();
        }

        int space = credentials.indexOf( ' ' );
        if( space <= 0 || space == credentials.length() - 1 )
        {
            return Optional.of( new Authentication( this.security.getAnonymousSubject(), AuthenticationResult.INVALID_CREDENTIALS ) );
        }

        String method = credentials.substring( 0, space );
        if( !"basic".equalsIgnoreCase( method ) )
        {
            return Optional.absent();
        }

        credentials = credentials.substring( space + 1 );
        credentials = new String( this.base64.decode( credentials ), Charsets.ISO_8859_1 );
        int i = credentials.indexOf( ':' );
        if( i <= 0 )
        {
            return Optional.of( new Authentication( this.security.getAnonymousSubject(), AuthenticationResult.INVALID_CREDENTIALS ) );
        }

        try
        {
            Subject subject = this.security.authenticate(
                    invocation.getApplication().getRealmName(),
                    invocation.getApplication().getPermissionPolicyName(),
                    new UsernamePasswordAuthToken( credentials.substring( 0, i ), credentials.substring( i + 1 ) )
            );
            return Optional.of( new Authentication( subject, AuthenticationResult.AUTHENTICATED ) );
        }
        catch( AuthenticationException e )
        {
            return Optional.of( new Authentication( this.security.getAnonymousSubject(), AuthenticationResult.AUTHENTICATION_FAILED ) );
        }
    }

    @Override
    public void challange( @Nonnull WebInvocation invocation )
    {
        invocation.getHttpResponse().setWwwAuthenticate( "basic realm=\"" + invocation.getApplication().getRealmName() + "\"" );
        invocation.getHttpResponse().setStatus( HttpStatus.UNAUTHORIZED, "Access denied" );
    }
}
