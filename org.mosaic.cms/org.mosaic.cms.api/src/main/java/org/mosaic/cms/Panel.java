package org.mosaic.cms;

import java.util.Collection;
import org.mosaic.cms.support.*;

/**
 * @author arik
 */
public interface Panel extends Named, PropertiesProvider, DataContainer, Secured, Filtered
{
    Block getBlock( String name );

    Collection<Block> getBlocks();
}
