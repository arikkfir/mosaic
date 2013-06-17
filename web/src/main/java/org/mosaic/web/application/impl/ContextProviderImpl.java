package org.mosaic.web.application.impl;

import javax.annotation.Nonnull;
import org.mosaic.util.collect.LinkedHashMapEx;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.collect.UnmodifiableMapEx;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.util.xml.XmlElement;
import org.mosaic.web.application.ContextProvider;

/**
 * @author arik
 */
public class ContextProviderImpl implements ContextProvider
{
    @Nonnull
    private final String name;

    @Nonnull
    private final String type;

    @Nonnull
    private final MapEx<String, String> parameters;

    public ContextProviderImpl( @Nonnull ConversionService conversionService, @Nonnull XmlElement element )
    {
        this.name = element.requireAttribute( "name" );
        this.type = element.requireAttribute( "type" );

        MapEx<String, String> parameters = new LinkedHashMapEx<>( 10, conversionService );
        for( XmlElement parameterElement : element.getChildElements( "parameter" ) )
        {
            parameters.put( parameterElement.requireAttribute( "name" ), parameterElement.getAttribute( "value" ) );
        }
        this.parameters = new UnmodifiableMapEx<>( parameters );
    }

    @Nonnull
    @Override
    public String getName()
    {
        return this.name;
    }

    @Nonnull
    @Override
    public String getType()
    {
        return this.type;
    }

    @Nonnull
    @Override
    public MapEx<String, String> getParameters()
    {
        return this.parameters;
    }
}
