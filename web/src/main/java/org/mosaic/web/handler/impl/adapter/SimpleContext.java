package org.mosaic.web.handler.impl.adapter;

import javax.annotation.Nonnull;
import org.mosaic.web.request.WebRequest;

/**
 * @author arik
 */
public class SimpleContext implements HandlerContext
{
    @Nonnull
    private final WebRequest request;

    public SimpleContext( @Nonnull WebRequest request )
    {
        this.request = request;
    }

    @Nonnull
    @Override
    public WebRequest getRequest()
    {
        return this.request;
    }
}
