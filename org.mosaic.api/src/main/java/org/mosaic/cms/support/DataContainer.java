package org.mosaic.cms.support;

import java.util.Map;
import org.mosaic.cms.DataProvider;

/**
 * @author arik
 */
public interface DataContainer
{
    DataProvider getDataProvider( String name );

    Map<String, DataProvider> getDataProviders();
}
