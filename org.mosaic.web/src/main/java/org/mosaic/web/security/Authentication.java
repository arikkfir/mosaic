package org.mosaic.web.security;

import javax.annotation.Nonnull;
import org.mosaic.modules.Service;
import org.mosaic.security.Security;
import org.mosaic.security.Subject;

/**
 * @author arik
 */
public final class Authentication
{
    @Nonnull
    private final Subject subject;

    @Nonnull
    private final AuthenticationResult result;

    @Nonnull
    @Service
    private Security security;

    public Authentication( @Nonnull AuthenticationResult result )
    {
        if( result == AuthenticationResult.AUTHENTICATED )
        {
            throw new IllegalArgumentException( "cannot return AUTHENTICATED result with no subject" );
        }
        this.subject = this.security.getAnonymousSubject();
        this.result = result;
    }

    public Authentication( @Nonnull Subject subject, @Nonnull AuthenticationResult result )
    {
        this.subject = subject;
        this.result = result;
    }

    @Nonnull
    public Subject getSubject()
    {
        return this.subject;
    }

    @Nonnull
    public AuthenticationResult getResult()
    {
        return this.result;
    }
}
