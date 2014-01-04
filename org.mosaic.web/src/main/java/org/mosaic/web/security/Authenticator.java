package org.mosaic.web.security;

import javax.annotation.Nonnull;
import org.mosaic.web.request.WebInvocation;

/**
 * @author arik
 */
public interface Authenticator
{
    @Nonnull
    String getAuthenticationMethod();

    @Nonnull
    Authentication authenticate( @Nonnull WebInvocation request );
}
