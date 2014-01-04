package org.mosaic.web.security.impl;

import javax.annotation.Nonnull;
import org.mosaic.modules.Service;
import org.mosaic.security.Security;
import org.mosaic.security.Subject;
import org.mosaic.web.request.WebInvocation;
import org.mosaic.web.request.WebSession;
import org.mosaic.web.security.Authentication;
import org.mosaic.web.security.AuthenticationResult;
import org.mosaic.web.security.Authenticator;

/**
 * @author arik
 */
@Service(properties = @Service.P(key = "type", value = "session"))
final class SessionAuthenticator implements Authenticator
{
    @Nonnull
    @Service
    private Security security;

    @Nonnull
    @Override
    public String getAuthenticationMethod()
    {
        return "session";
    }

    @Nonnull
    public Authentication authenticate( @Nonnull WebInvocation request )
    {
        WebSession session = request.getSession();
        if( session != null )
        {
            Subject subject = session.getAttributes().get( SessionAuthenticator.class.getName() + "#subject", Subject.class );
            if( subject != null )
            {
                return new Authentication( subject, AuthenticationResult.AUTHENTICATED );
            }
        }
        return new Authentication( AuthenticationResult.NO_CREDENTIALS );
    }
}
