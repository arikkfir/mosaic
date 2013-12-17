package org.mosaic.web.handler;

import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.web.request.WebRequest;

/**
 * @author arik
 */
public interface RequestHandler
{
    @Nonnull
    Set<String> getHttpMethods();

    boolean canHandle( @Nonnull WebRequest request );

    @Nullable
    Object handle( @Nonnull WebRequest request ) throws Throwable;
}
