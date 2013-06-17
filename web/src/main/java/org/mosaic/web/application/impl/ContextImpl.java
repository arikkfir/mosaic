package org.mosaic.web.application.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nonnull;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.util.xml.XmlElement;
import org.mosaic.web.application.ContextProvider;

/**
 * @author arik
 */
public class ContextImpl
{
    @Nonnull
    private final Collection<ContextProvider> contextProviders;

    public ContextImpl()
    {
        this.contextProviders = Collections.emptyList();
    }

    public ContextImpl( @Nonnull ConversionService conversionService, @Nonnull XmlElement element )
    {
        List<ContextProvider> contextProviders = new LinkedList<>();
        for( XmlElement providerElement : element.getChildElements( "provider" ) )
        {
            contextProviders.add( new ContextProviderImpl( conversionService, providerElement ) );
        }
        this.contextProviders = Collections.unmodifiableList( contextProviders );
    }

    @Nonnull
    public Collection<ContextProvider> getContextProviders()
    {
        return this.contextProviders;
    }
}
