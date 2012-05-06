package org.mosaic.cms;

import java.util.Collection;
import org.mosaic.cms.support.DataContainer;
import org.mosaic.cms.support.Filtered;
import org.mosaic.cms.support.Named;
import org.mosaic.cms.support.PropertiesProvider;

/**
 * @author arik
 */
public interface Site extends DataContainer, Filtered, Named, PropertiesProvider
{
    Blueprint getBlueprint( String name );

    Collection<Blueprint> getBlueprints();

    Page getPage( String name );

    Collection<Page> getPages();
}
