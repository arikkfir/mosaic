package org.mosaic.web.application.impl;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nonnull;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.util.xml.XmlElement;
import org.mosaic.web.application.ContextProviderRef;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

/**
 * @author arik
 */
public class ContextImpl
{
    @Nonnull
    private final Collection<ContextProviderRef> contextProviderRefs;

    public ContextImpl()
    {
        this.contextProviderRefs = emptyList();
    }

    public ContextImpl( @Nonnull ConversionService conversionService, @Nonnull XmlElement element )
    {
        List<ContextProviderRef> contextProviderRefs = new LinkedList<>();
        for( XmlElement providerElement : element.getChildElements( "provider" ) )
        {
            contextProviderRefs.add( new ContextProviderRefImpl( conversionService, providerElement ) );
        }
        this.contextProviderRefs = unmodifiableList( contextProviderRefs );
    }

    @Nonnull
    public Collection<ContextProviderRef> getContextProviderRefs()
    {
        return this.contextProviderRefs;
    }
}
