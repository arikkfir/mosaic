package org.mosaic.server.cms.impl.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.mosaic.cms.Block;
import org.mosaic.cms.Panel;

/**
 * @author arik
 */
public class PanelImpl extends BaseModel implements Panel
{
    private Map<String, BlockImpl> blocks = new HashMap<>();

    @Override
    public Block getBlock( String name )
    {
        return this.blocks.get( name );
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public Collection<Block> getBlocks()
    {
        return ( Collection ) this.blocks.values();
    }

    public synchronized void addBlock( BlockImpl block )
    {
        Map<String, BlockImpl> newBlocks = new HashMap<>( this.blocks );
        newBlocks.put( block.getName(), block );
        this.blocks = newBlocks;
    }
}
