package org.mosaic.web.handler;

import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.web.request.WebInvocation;

/**
 * @author arik
 */
public interface RequestHandler
{
    @Nonnull
    Set<String> getHttpMethods();

    boolean canHandle( @Nonnull WebInvocation request );

    @Nullable
    Object handle( @Nonnull WebInvocation request ) throws Throwable;
}
