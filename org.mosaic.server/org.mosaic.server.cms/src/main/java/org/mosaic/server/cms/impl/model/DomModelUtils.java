package org.mosaic.server.cms.impl.model;

import java.util.HashMap;
import java.util.Map;
import org.mosaic.server.cms.impl.DataProviderRegistry;
import org.mosaic.server.util.xml.DomUtils;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.w3c.dom.Element;

/**
 * @author arik
 */
public abstract class DomModelUtils
{
    public static void setNames( BaseModel container, Element containerElement )
    {
        String name = containerElement.getAttribute( "id" );
        if( name == null )
        {
            name = containerElement.getAttribute( "name" );
        }
        setNames( container, name, containerElement );
    }

    public static void setNames( BaseModel container, String name, Element containerElement )
    {
        container.setName( name );
        if( containerElement.hasAttribute( "display-name" ) )
        {
            container.setDisplayName( containerElement.getAttribute( "display-name" ) );
        }
    }

    public static void setProperties( BaseModel container, Element containerElement )
    {
        Map<String, String> properties = new HashMap<>( 10 );
        for( Element dataElement : DomUtils.getChildElements( containerElement, "properties" ) )
        {
            for( Element propertyElement : DomUtils.getChildElements( dataElement ) )
            {
                properties.put( propertyElement.getLocalName(), propertyElement.getTextContent().trim() );
            }
        }
        container.setProperties( properties );
    }

    public static void addDataProviders( BaseModel container,
                                         Element containerElement,
                                         DataProviderRegistry dataProviderRegistry )
    {
        for( Element dataElement : DomUtils.getChildElements( containerElement, "data" ) )
        {
            for( Element providerElement : DomUtils.getChildElements( dataElement, "provider" ) )
            {
                container.addDataProvider( new DataProviderImpl( dataProviderRegistry, providerElement ) );
            }
        }
    }

    public static void setSecurityExpression( BaseModel container, Element containerElement )
    {
        if( containerElement.hasAttribute( "security" ) )
        {
            String expr = containerElement.getAttribute( "security" );
            Expression expression = new SpelExpressionParser().parseExpression( expr );
            container.setSecurityExpression( expression );
        }
    }
}
