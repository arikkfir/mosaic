package org.mosaic.web.handler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.web.request.WebRequest;

/**
 * @author arik
 */
public interface SecuredRequestHandler extends RequestHandler
{
    @Nullable
    String getAuthenticationMethod( @Nonnull WebRequest request );
}
