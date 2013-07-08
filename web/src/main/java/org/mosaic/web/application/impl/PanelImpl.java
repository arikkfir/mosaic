package org.mosaic.web.application.impl;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.xpath.XPathException;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.util.xml.XmlElement;
import org.mosaic.web.application.*;

import static java.util.Collections.unmodifiableList;

/**
 * @author arik
 */
public class PanelImpl implements Panel
{
    @Nonnull
    private final Template template;

    @Nonnull
    private final String name;

    @Nullable
    private final String displayName;

    @Nonnull
    private final ContextImpl context;

    @Nonnull
    private final List<Block> blocks;

    public PanelImpl( @Nonnull ConversionService conversionService,
                      @Nonnull Template template,
                      @Nonnull XmlElement element )
            throws XPathException, WebApplicationParseException
    {
        this.template = template;
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

        List<Block> blocks = new LinkedList<>();
        for( XmlElement blockElement : element.findElements( "c:blocks/c:block" ) )
        {
            blocks.add( new BlockImpl( conversionService, this, blockElement ) );
        }
        this.blocks = unmodifiableList( blocks );
    }

    @Nonnull
    @Override
    public Template getTemplate()
    {
        return this.template;
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
    public List<Block> getBlocks()
    {
        return this.blocks;
    }
}
