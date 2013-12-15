package org.mosaic.web.handler;

import javax.annotation.Nonnull;
import org.mosaic.web.request.WebRequest;

/**
 * @author arik
 */
public interface RequestHandler
{
    boolean canHandle( @Nonnull WebRequest request );

    void handle( @Nonnull WebRequest request ) throws Throwable;
}
