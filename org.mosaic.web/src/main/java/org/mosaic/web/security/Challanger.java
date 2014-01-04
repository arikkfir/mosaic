package org.mosaic.web.security;

import javax.annotation.Nonnull;
import org.mosaic.web.request.WebInvocation;

/**
 * @author arik
 */
public interface Challanger
{
    @Nonnull
    String getAuthenticationMethod();

    void challange( @Nonnull WebInvocation request );
}
