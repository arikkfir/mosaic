package org.mosaic.config;

import org.mosaic.util.collection.MapAccessor;

/**
 * @author arik
 */
public interface Configuration extends MapAccessor<String, String>
{
    String getName();
}
