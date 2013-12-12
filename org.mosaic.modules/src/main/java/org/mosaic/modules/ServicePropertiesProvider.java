package org.mosaic.modules;

import java.util.Dictionary;
import javax.annotation.Nonnull;

/**
 * @author arik
 */
public interface ServicePropertiesProvider
{
    void addProperties( @Nonnull Dictionary<String, Object> properties );
}
