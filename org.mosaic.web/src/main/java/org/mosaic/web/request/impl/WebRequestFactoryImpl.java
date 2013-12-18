package org.mosaic.web.request.impl;

import javax.annotation.Nonnull;
import org.eclipse.jetty.server.Request;
import org.mosaic.modules.Component;
import org.mosaic.web.application.Application;
import org.mosaic.web.request.WebRequest;
import org.mosaic.web.request.spi.WebRequestFactory;

/**
 * @author arik
 */
@Component
final class WebRequestFactoryImpl implements WebRequestFactory
{
    @Nonnull
    @Override
    public WebRequest createRequest( @Nonnull Application application, @Nonnull Object request )
    {
        return new WebRequestImpl( application, ( Request ) request );
    }
}
