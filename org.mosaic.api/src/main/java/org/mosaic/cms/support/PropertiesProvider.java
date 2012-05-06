package org.mosaic.cms.support;

import org.mosaic.util.collection.MapAccessor;

/**
 * @author arik
 */
public interface PropertiesProvider
{
    MapAccessor<String, String> getProperties();
}
