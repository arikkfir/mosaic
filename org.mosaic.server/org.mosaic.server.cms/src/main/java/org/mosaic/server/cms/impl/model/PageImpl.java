package org.mosaic.server.cms.impl.model;

import java.util.*;
import org.mosaic.cms.Block;
import org.mosaic.cms.Blueprint;
import org.mosaic.cms.Page;
import org.mosaic.cms.Site;
import org.mosaic.util.collection.MultiMapAccessor;
import org.mosaic.util.collection.MultiMapWrapper;

/**
 * @author arik
 */
public class PageImpl extends BaseModel implements Page
{
    private Site site;

    private Blueprint blueprint;

    private boolean enabled;

    private Long defaultExpiration;

    private final MultiMapWrapper<String, String> urls = new MultiMapWrapper<>();

    private Map<String, BlockImpl> blocks = new HashMap<>();

    private Collection<String> tags = Collections.emptyList();

    @Override
    public Site getSite()
    {
        return this.site;
    }

    public void setSite( Site site )
    {
        this.site = site;
    }

    @Override
    public Blueprint getBlueprint()
    {
        return this.blueprint;
    }

    public void setBlueprint( Blueprint blueprint )
    {
        this.blueprint = blueprint;
    }

    @Override
    public boolean isEnabled()
    {
        return this.enabled;
    }

    public void setEnabled( boolean enabled )
    {
        this.enabled = enabled;
    }

    @Override
    public Long getDefaultExpiration()
    {
        return this.defaultExpiration;
    }

    public void setDefaultExpiration( Long defaultExpiration )
    {
        this.defaultExpiration = defaultExpiration;
    }

    @Override
    public MultiMapAccessor<String, String> getUrls()
    {
        return this.urls;
    }

    public synchronized void setUrls( Map<String, List<String>> urls )
    {
        this.urls.setMap( Collections.unmodifiableMap( new HashMap<>( urls ) ) );
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

    public synchronized void addBlock( BlockImpl block )
    {
        Map<String, BlockImpl> newBlocks = new HashMap<>( this.blocks );
        newBlocks.put( block.getName(), block );
        this.blocks = newBlocks;
    }

    @Override
    public Collection<String> getTags()
    {
        return this.tags;
    }

    public void setTags( Collection<String> tags )
    {
        this.tags = Collections.unmodifiableCollection( new LinkedList<>( tags ) );
    }
}
