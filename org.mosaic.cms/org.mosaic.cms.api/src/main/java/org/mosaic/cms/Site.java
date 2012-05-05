package org.mosaic.cms;

import java.util.Collection;
import org.mosaic.cms.support.*;

/**
 * @author arik
 */
public interface Site extends DataContainer, Filtered, Named, PropertiesProvider, Secured
{
    Blueprint getBlueprint( String name );

    Collection<Blueprint> getBlueprints();

    Page getPage( String name );

    Collection<Page> getPages();
}
