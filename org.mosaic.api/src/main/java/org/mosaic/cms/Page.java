package org.mosaic.cms;

import java.util.Collection;
import org.mosaic.cms.support.DataContainer;
import org.mosaic.cms.support.Named;
import org.mosaic.cms.support.PropertiesProvider;
import org.mosaic.cms.support.Secured;
import org.mosaic.util.collection.MultiMapAccessor;

/**
 * @author arik
 */
public interface Page extends Named, PropertiesProvider, DataContainer, Secured
{
    Site getSite();

    Blueprint getBlueprint();

    boolean isEnabled();

    Long getDefaultExpiration();

    MultiMapAccessor<String, String> getUrls();

    Block getBlock( String name );

    Collection<Block> getBlocks();

    Collection<String> getTags();
}
