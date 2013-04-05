package org.mosaic.web.client;

import javax.annotation.Nonnull;

/**
 * @author arik
 */
public interface RequestFactory
{
    @Nonnull
    Request createRequest();
}
