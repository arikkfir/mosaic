package org.mosaic.server.cms.impl.model;

import org.mosaic.cms.Block;
import org.mosaic.server.cms.impl.DataProviderRegistry;
import org.w3c.dom.Element;

/**
 * @author arik
 */
public class BlockImpl extends BaseModel implements Block
{
    private final String snippet;

    public BlockImpl( Element blockElement, DataProviderRegistry dataProviderRegistry )
    {
        DomModelUtils.setNames( this, blockElement );
        DomModelUtils.setProperties( this, blockElement );
        DomModelUtils.addDataProviders( this, blockElement, dataProviderRegistry );
        DomModelUtils.setSecurityExpression( this, blockElement );
        this.snippet = blockElement.getAttribute( "snippet" );
    }

    @Override
    public String getSnippet()
    {
        return this.snippet;
    }
}
