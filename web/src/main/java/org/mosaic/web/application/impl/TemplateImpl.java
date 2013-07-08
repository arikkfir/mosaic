package org.mosaic.web.application.impl;

import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.xpath.XPathException;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.util.xml.XmlElement;
import org.mosaic.web.application.*;

import static java.util.Collections.unmodifiableMap;

/**
 * @author arik
 */
public class TemplateImpl implements Template
{
    @Nonnull
    private final WebApplication application;

    @Nonnull
    private final String name;

    @Nullable
    private final String displayName;

    @Nonnull
    private final Snippet snippet;

    @Nonnull
    private final ContextImpl context;

    @Nonnull
    private final Map<String, Panel> panels;

    public TemplateImpl( @Nonnull ConversionService conversionService,
                         @Nonnull WebApplication application,
                         @Nonnull XmlElement element ) throws XPathException, WebApplicationParseException
    {
        this.application = application;
        this.name = element.requireAttribute( "name" );
        this.displayName = element.getAttribute( "display-name" );

        Snippet snippet = this.application.getSnippet( getSnippetName() );
        if( snippet == null )
        {
            this.snippet = new SnippetImpl( getSnippetName(), "" );
        }
        else
        {
            this.snippet = snippet;
        }

        XmlElement contextElement = element.getFirstChildElement( "context" );
        if( contextElement != null )
        {
            this.context = new ContextImpl( conversionService, contextElement );
        }
        else
        {
            this.context = new ContextImpl();
        }

        Map<String, Panel> panels = new LinkedHashMap<>( 5 );
        for( XmlElement blockElement : element.findElements( "c:panels/c:panel" ) )
        {
            PanelImpl panel = new PanelImpl( conversionService, this, blockElement );
            panels.put( panel.getName(), panel );
        }
        this.panels = unmodifiableMap( panels );

        // validate no duplicate block names
        Set<String> blockNames = new HashSet<>();
        for( Panel panel : this.panels.values() )
        {
            for( Block block : panel.getBlocks() )
            {
                if( blockNames.contains( block.getName() ) )
                {
                    throw new WebApplicationParseException( "Template '" + this.name + "' contains duplicate block names ('" + block.getName() + "')" );
                }
                blockNames.add( block.getName() );
            }
        }
    }

    private String getSnippetName()
    {
        return "___" + this.name;
    }

    @Nonnull
    @Override
    public WebApplication getApplication()
    {
        return this.application;
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

    @Nullable
    @Override
    public Panel getPanel( @Nonnull String name )
    {
        return this.panels.get( name );
    }

    @Nonnull
    @Override
    public Collection<Panel> getPanels()
    {
        return this.panels.values();
    }
}
