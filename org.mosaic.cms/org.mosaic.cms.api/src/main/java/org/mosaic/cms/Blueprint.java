package org.mosaic.cms;

import java.util.Collection;
import org.mosaic.cms.support.*;

/**
 * @author arik
 */
public interface Blueprint extends Named, PropertiesProvider, DataContainer, Secured, Filtered
{
    Site getSite();

    Blueprint getParent();

    Panel getPanel( String name );

    Collection<Panel> getPanels();
}
