package org.mosaic.cms.sites.impl.model;

import org.mosaic.cms.Block;

/**
 * @author arik
 */
public class BlockImpl extends BaseModel implements Block
{
    private String snippet;

    @Override
    public String getSnippet()
    {
        return this.snippet;
    }

    public void setSnippet( String snippet )
    {
        this.snippet = snippet;
    }
}
