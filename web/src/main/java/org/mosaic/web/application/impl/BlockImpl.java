package org.mosaic.web.application.impl;

import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.util.xml.XmlElement;
import org.mosaic.web.application.Block;
import org.mosaic.web.application.ContextProviderRef;
import org.mosaic.web.application.Page;
import org.mosaic.web.application.Snippet;

/**
 * @author arik
 */
public class BlockImpl implements Block
{
    @Nonnull
    private final Page page;

    @Nonnull
    private final String name;

    @Nonnull
    private final ContextImpl context;

    @Nullable
    private String displayName;

    @Nonnull
    private Snippet snippet;

    public BlockImpl( @Nonnull ConversionService conversionService, @Nonnull Page page, @Nonnull XmlElement element )
    {
        this.page = page;
        this.name = element.requireAttribute( "name" );
        this.displayName = element.getAttribute( "display-name" );

        XmlElement contextElement = element.getFirstChildElement( "context" );
        if( contextElement != null )
        {
            this.context = new ContextImpl( conversionService, contextElement );
        }
        else
        {
            this.context = new ContextImpl();
        }

        String snippetId = element.requireAttribute( "snippet-id" );
        Snippet snippet = this.page.getApplication().getSnippetMap().get( snippetId );
        if( snippet == null )
        {
            throw new IllegalStateException( "Block '" + this.name + "' in page '" + this.page.getName() + "' uses unknown snippet: " + snippetId );
        }
        this.snippet = snippet;
    }

    @Nonnull
    @Override
    public Page getPage()
    {
        return this.page;
    }

    @Nonnull
    @Override
    public String getName()
    {
        return this.name;
    }

    @Nonnull
    @Override
    public String getDisplayName()
    {
        return this.displayName == null ? getName() : this.displayName;
    }

    @Nonnull
    @Override
    public Collection<ContextProviderRef> getContext()
    {
        return this.context.getContextProviderRefs();
    }

    @Nonnull
    @Override
    public Snippet getSnippet()
    {
        return this.snippet;
    }
}
