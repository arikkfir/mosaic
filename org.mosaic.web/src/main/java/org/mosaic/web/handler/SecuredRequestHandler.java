package org.mosaic.web.handler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.web.request.WebInvocation;
import org.mosaic.web.security.SecurityConstraint;

/**
 * @author arik
 */
public interface SecuredRequestHandler extends RequestHandler
{
    @Nullable
    SecurityConstraint getSecurityConstraint( @Nonnull WebInvocation request );
}
