package org.mosaic.web.application.impl;

import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.util.xml.XmlElement;
import org.mosaic.web.application.*;

/**
 * @author arik
 */
public class BlockImpl implements Block
{
    @Nonnull
    private final Panel panel;

    @Nonnull
    private final String name;

    @Nullable
    private final String displayName;

    @Nonnull
    private final Snippet snippet;

    @Nonnull
    private final ContextImpl context;

    public BlockImpl( @Nonnull ConversionService conversionService, @Nonnull Panel panel, @Nonnull XmlElement element )
            throws WebApplicationParseException
    {
        this.panel = panel;
        this.name = element.requireAttribute( "name" );
        this.displayName = element.getAttribute( "display-name" );

        String snippetId = element.requireAttribute( "snippet" );
        Snippet snippet = this.panel.getTemplate().getWebContent().getSnippet( snippetId );
        if( snippet == null )
        {
            throw new WebApplicationParseException( "Block '" + this + "' uses unknown snippet: " + snippetId );
        }
        this.snippet = snippet;

        XmlElement contextElement = element.getFirstChildElement( "context" );
        if( contextElement != null )
        {
            this.context = new ContextImpl( conversionService, contextElement );
        }
        else
        {
            this.context = new ContextImpl();
        }
    }

    @Override
    public String toString()
    {
        return "Block[" + this.name + "]";
    }

    @Nonnull
    @Override
    public Panel getPanel()
    {
        return this.panel;
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
    public Snippet getSnippet()
    {
        return this.snippet;
    }

    @Nonnull
    @Override
    public Collection<ContextProviderRef> getContext()
    {
        return this.context.getContextProviderRefs();
    }
}
