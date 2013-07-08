package org.mosaic.web.application;

import javax.annotation.Nonnull;

/**
 * @author arik
 */
public interface Snippet
{
    @Nonnull
    String getName();

    @Nonnull
    String getContent();
}
