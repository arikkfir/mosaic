package org.mosaic.cms.support;

import java.util.Map;
import org.mosaic.cms.DataProvider;

/**
 * @author arik
 */
public interface DataContainer
{
    Map<String, DataProvider> getDataProviders();
}
