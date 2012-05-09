package org.mosaic.server.cms.impl.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.mosaic.cms.Block;
import org.mosaic.cms.Panel;
import org.mosaic.server.cms.impl.DataProviderRegistry;
import org.mosaic.server.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * @author arik
 */
public class PanelImpl extends BaseModel implements Panel
{
    private final Map<String, BlockImpl> blocks = new HashMap<>();

    public PanelImpl( Element panelElement, DataProviderRegistry dataProviderRegistry )
    {
        DomModelUtils.setNames( this, panelElement );
        DomModelUtils.setProperties( this, panelElement );
        DomModelUtils.addDataProviders( this, panelElement, dataProviderRegistry );
        DomModelUtils.setSecurityExpression( this, panelElement );

        for( Element blockElement : DomUtils.getChildElements( panelElement, "block" ) )
        {
            BlockImpl block = new BlockImpl( blockElement, dataProviderRegistry );
            this.blocks.put( block.getName(), block );
        }
    }

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
}
