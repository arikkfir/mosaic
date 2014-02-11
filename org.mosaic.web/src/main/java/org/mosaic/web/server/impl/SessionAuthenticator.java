package org.mosaic.web.server.impl;

import com.google.common.base.Optional;
import javax.annotation.Nonnull;
import org.mosaic.modules.Service;
import org.mosaic.security.Security;
import org.mosaic.security.Subject;
import org.mosaic.web.server.*;

/**
 * @author arik
 */
@Service(properties = @Service.P(key = "type", value = "session"))
final class SessionAuthenticator implements Authenticator
{

    private static final String SUBJECT_KEY = SessionAuthenticator.class.getName() + "#subject";

    @Nonnull
    @Service
    private Security security;

    @Nonnull
    public Optional<Authentication> authenticate( @Nonnull WebInvocation invocation )
    {
        WebSession session = invocation.getSession();
        if( session != null )
        {
            Optional<Subject> subject = session.getAttributes().find( SUBJECT_KEY, Subject.class );
            if( subject.isPresent() )
            {
                return Optional.of( new Authentication( subject.get(), AuthenticationResult.AUTHENTICATED ) );
            }
        }
        return Optional.absent();
    }
}
