package org.mosaic.web.server;

import javax.annotation.Nonnull;
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
