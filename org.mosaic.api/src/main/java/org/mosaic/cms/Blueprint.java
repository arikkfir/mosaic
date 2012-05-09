package org.mosaic.cms;

import java.util.Collection;
import org.mosaic.cms.support.DataContainer;
import org.mosaic.cms.support.Named;
import org.mosaic.cms.support.PropertiesProvider;
import org.mosaic.cms.support.Secured;

/**
 * @author arik
 */
public interface Blueprint extends Named, PropertiesProvider, DataContainer, Secured
{
    Site getSite();

    Blueprint getParent();

    Panel getPanel( String name );

    Collection<Panel> getPanels();
}
