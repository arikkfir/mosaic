package org.mosaic.web.application.impl;

import java.util.*;
import javax.annotation.Nonnull;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.util.xml.XmlElement;
import org.mosaic.web.application.ContextProviderRef;

import static java.util.Collections.*;

/**
 * @author arik
 */
public class ContextImpl
{
    @Nonnull
    public static Collection<ContextProviderRef> getContextProviderRefs(
            @Nonnull ConversionService conversionService,
            @Nonnull Collection<ContextProviderRef> prexistingProviders,
            @Nonnull XmlElement element )
    {
        Map<String, ContextProviderRef> contextProviderRefs = new LinkedHashMap<>( 10 );
        for( ContextProviderRef contextProviderRef : prexistingProviders )
        {
            contextProviderRefs.put( contextProviderRef.getName(), contextProviderRef );
        }
        for( ContextProviderRef contextProviderRef : getContextProviderRefs( conversionService, element ) )
        {
            contextProviderRefs.put( contextProviderRef.getName(), contextProviderRef );
        }
        return unmodifiableCollection( contextProviderRefs.values() );
    }

    @Nonnull
    public static Collection<ContextProviderRef> getContextProviderRefs( @Nonnull ConversionService conversionService,
                                                                         @Nonnull XmlElement element )
    {
        XmlElement contextElement = element.getFirstChildElement( "context" );
        if( contextElement != null )
        {
            List<ContextProviderRef> contextProviderRefs = new LinkedList<>();
            for( XmlElement providerElement : contextElement.getChildElements( "provider" ) )
            {
                ContextProviderRefImpl contextProviderRef = new ContextProviderRefImpl( conversionService, providerElement );
                contextProviderRefs.add( contextProviderRef );
            }
            return contextProviderRefs;
        }
        else
        {
            return emptyList();
        }
    }

    @Nonnull
    private final Collection<ContextProviderRef> contextProviderRefs;

    public ContextImpl()
    {
        this.contextProviderRefs = emptyList();
    }

    public ContextImpl( @Nonnull ConversionService conversionService, @Nonnull XmlElement element )
    {
        this( conversionService, Collections.<ContextProviderRef>emptyList(), element );
    }

    public ContextImpl( @Nonnull ConversionService conversionService,
                        @Nonnull Collection<ContextProviderRef> moreContextProviderRefs,
                        @Nonnull XmlElement element )
    {
        List<ContextProviderRef> contextProviderRefs = new LinkedList<>( moreContextProviderRefs );
        for( XmlElement providerElement : element.getChildElements( "provider" ) )
        {
            contextProviderRefs.add( new ContextProviderRefImpl( conversionService, providerElement ) );
        }
        this.contextProviderRefs = unmodifiableList( contextProviderRefs );
    }
}
