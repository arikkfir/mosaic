package org.mosaic.cms;

import org.mosaic.cms.support.DataContainer;
import org.mosaic.cms.support.Named;
import org.mosaic.cms.support.PropertiesProvider;
import org.mosaic.cms.support.Secured;

/**
 * @author arik
 */
public interface Block extends Named, PropertiesProvider, DataContainer, Secured
{
    String getSnippet();
}
