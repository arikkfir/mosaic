package org.mosaic.web.server;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public interface RequestHandler
{
    boolean canHandle( @Nonnull WebInvocation request );

    @Nullable
    Object handle( @Nonnull WebInvocation invocation ) throws Throwable;
}
