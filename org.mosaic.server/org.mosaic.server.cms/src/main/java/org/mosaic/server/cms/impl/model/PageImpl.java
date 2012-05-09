package org.mosaic.server.cms.impl.model;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FilenameUtils;
import org.mosaic.cms.Block;
import org.mosaic.cms.Blueprint;
import org.mosaic.cms.Page;
import org.mosaic.cms.Site;
import org.mosaic.server.util.xml.DomUtils;
import org.mosaic.util.collection.MultiMapAccessor;
import org.mosaic.util.collection.MultiMapWrapper;
import org.w3c.dom.Element;

import static java.lang.String.format;

/**
 * @author arik
 */
public class PageImpl extends BaseModel implements Page
{
    private final Site site;

    private final Blueprint blueprint;

    private final boolean enabled;

    private final Long defaultExpiration;

    private final MultiMapWrapper<String, String> urls = new MultiMapWrapper<>();

    private final Map<String, BlockImpl> blocks = new HashMap<>();

    private final Collection<String> tags = Collections.emptyList();

    public PageImpl( SiteImpl site, File pageFile )
    {
        this.site = site;

        String name = FilenameUtils.getBaseName( pageFile.getName() );
        Element pageElement;
        try
        {
            pageElement = DomUtils.parseDocument( pageFile.toURI() ).getDocumentElement();
        }
        catch( Exception e )
        {
            site.addError( format( "Could not read or parse page file '%s': %s", pageFile, e.getMessage() ), e );
            this.blueprint = null;
            this.enabled = false;
            this.defaultExpiration = null;
            return;
        }

        DomModelUtils.setNames( this, name, pageElement );
        DomModelUtils.setProperties( this, pageElement );
        DomModelUtils.addDataProviders( this, pageElement, site.getDataProviderRegistry() );
        DomModelUtils.setSecurityExpression( this, pageElement );
        this.blueprint = pageElement.hasAttribute( "blueprint" )
                         ? site.getBlueprint( pageElement.getAttribute( "blueprint" ) )
                         : null;
        this.enabled = !pageElement.hasAttribute( "enabled" ) || Boolean.valueOf( pageElement.getAttribute( "enabled" ) );
        this.defaultExpiration = pageElement.hasAttribute( "expiration" )
                                 ? Long.valueOf( pageElement.getAttribute( "expiration" ) )
                                 : null;

        for( Element urlsElement : DomUtils.getChildElements( pageElement, "urls" ) )
        {
            for( Element langElement : DomUtils.getChildElements( urlsElement ) )
            {
                for( Element urlElement : DomUtils.getChildElements( langElement, "url" ) )
                {
                    this.urls.add( langElement.getLocalName(), urlElement.getTextContent().trim() );
                }
            }
        }

        for( Element blockElement : DomUtils.getChildElements( pageElement, "block" ) )
        {
            BlockImpl block = new BlockImpl( blockElement, site.getDataProviderRegistry() );
            this.blocks.put( block.getName(), block );
        }
    }

    @Override
    public Site getSite()
    {
        return this.site;
    }

    @Override
    public Blueprint getBlueprint()
    {
        return this.blueprint;
    }

    @Override
    public boolean isEnabled()
    {
        return this.enabled;
    }

    @Override
    public Long getDefaultExpiration()
    {
        return this.defaultExpiration;
    }

    @Override
    public MultiMapAccessor<String, String> getUrls()
    {
        return this.urls;
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

    @Override
    public Collection<String> getTags()
    {
        return this.tags;
    }
}
