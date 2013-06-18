package org.mosaic.web.application;

import javax.annotation.Nonnull;
import org.mosaic.util.collect.MapEx;

/**
 * @author arik
 */
public interface ContextProvider
{
    @Nonnull
    String getName();

    @Nonnull
    String getType();

    @Nonnull
    MapEx<String, String> getParameters();
}