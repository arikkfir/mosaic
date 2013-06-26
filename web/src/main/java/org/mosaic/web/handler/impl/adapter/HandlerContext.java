package org.mosaic.web.handler.impl.adapter;

import javax.annotation.Nonnull;
import org.mosaic.web.request.WebRequest;

/**
 * @author arik
 */
public interface HandlerContext
{
    @Nonnull
    WebRequest getRequest();
}
