package org.mosaic.cms;

import java.util.Collection;
import org.mosaic.cms.support.*;
import org.mosaic.util.collection.MultiMapAccessor;

/**
 * @author arik
 */
public interface Page extends Named, PropertiesProvider, DataContainer, Secured, Filtered
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
