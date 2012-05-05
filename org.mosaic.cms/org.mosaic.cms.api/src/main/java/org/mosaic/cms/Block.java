package org.mosaic.cms;

import org.mosaic.cms.support.*;

/**
 * @author arik
 */
public interface Block extends Named, PropertiesProvider, DataContainer, Secured, Filtered
{
    String getSnippet();
}
