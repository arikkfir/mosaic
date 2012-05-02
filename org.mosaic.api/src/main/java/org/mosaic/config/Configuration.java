package org.mosaic.config;

import java.util.Map;

/**
 * @author arik
 */
public interface Configuration extends Map<String, String>
{
    String getName();
}
