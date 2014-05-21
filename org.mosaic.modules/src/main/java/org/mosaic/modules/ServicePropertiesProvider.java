package org.mosaic.modules;

import java.util.Dictionary;
import org.mosaic.core.util.Nonnull;

/**
 * @author arik
 */
public interface ServicePropertiesProvider
{
    void addProperties( @Nonnull Dictionary<String, Object> properties );
}
