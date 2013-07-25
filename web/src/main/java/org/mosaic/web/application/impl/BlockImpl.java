package org.mosaic.web.application.impl;

import com.google.common.reflect.TypeToken;
import java.util.Collection;
import java.util.Collections;
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
    private final Collection<ContextProviderRef> context;

    public BlockImpl( @Nonnull ConversionService conversionService,
                      @Nonnull Panel panel,
                      @Nullable Block parentBlock,
                      @Nonnull XmlElement element )
            throws WebApplicationParseException
    {
        this.panel = panel;
        this.name = element.requireAttribute( "name" );
        this.displayName =
                parentBlock != null
                ? element.requireAttribute( "display-name", TypeToken.of( String.class ), parentBlock.getDisplayName() )
                : element.getAttribute( "display-name" );

        if( parentBlock != null )
        {
            String snippetId = element.getAttribute( "snippet" );
            if( snippetId == null )
            {
                this.snippet = parentBlock.getSnippet();
            }
            else
            {
                Snippet snippet = this.panel.getTemplate().getWebContent().getSnippet( snippetId );
                if( snippet == null )
                {
                    throw new WebApplicationParseException( "Block '" + this + "' uses unknown snippet: " + snippetId );
                }
                this.snippet = snippet;
            }
        }
        else
        {
            String snippetId = element.requireAttribute( "snippet" );
            Snippet snippet = this.panel.getTemplate().getWebContent().getSnippet( snippetId );
            if( snippet == null )
            {
                throw new WebApplicationParseException( "Block '" + this.name + "' uses unknown snippet: " + snippetId );
            }
            this.snippet = snippet;
        }

        Collection<ContextProviderRef> prexistingProviders =
                parentBlock == null
                ? Collections.<ContextProviderRef>emptyList()
                : parentBlock.getContext();
        this.context = ContextImpl.getContextProviderRefs( conversionService, prexistingProviders, element );
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
        return this.context;
    }
}
