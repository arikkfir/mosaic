package org.mosaic.web.server;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public interface Secured
{
    @Nullable
    SecurityConstraint getSecurityConstraint( @Nonnull WebInvocation request );
}
