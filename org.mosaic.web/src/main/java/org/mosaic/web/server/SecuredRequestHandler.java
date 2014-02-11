package org.mosaic.web.server;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public interface SecuredRequestHandler extends RequestHandler
{
    @Nullable
    SecurityConstraint getSecurityConstraint( @Nonnull WebInvocation request );
}
