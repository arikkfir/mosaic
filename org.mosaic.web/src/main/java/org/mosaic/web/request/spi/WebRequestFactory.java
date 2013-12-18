package org.mosaic.web.request.spi;

import javax.annotation.Nonnull;
import org.mosaic.web.application.Application;
import org.mosaic.web.request.WebRequest;

/**
 * @author arik
 */
public interface WebRequestFactory
{
    @Nonnull
    WebRequest createRequest( @Nonnull Application application, @Nonnull Object request );
}
