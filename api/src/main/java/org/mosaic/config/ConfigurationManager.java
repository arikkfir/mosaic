package org.mosaic.config;

import javax.annotation.Nonnull;
import org.mosaic.util.collect.MapEx;

/**
 * @author arik
 */
public interface ConfigurationManager
{
    @Nonnull
    MapEx<String, String> getConfiguration( @Nonnull String name );
}
