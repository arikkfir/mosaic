package org.mosaic.server.util.xml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * @author arik
 */
public abstract class DomUtils
{
    private static final StrictErrorHandler STRICT_ERROR_HANDLER = new StrictErrorHandler();

    public static Document parseDocument( Path path ) throws ParserConfigurationException, IOException, SAXException
    {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware( true );

        DocumentBuilder db = dbf.newDocumentBuilder();
        db.setErrorHandler( STRICT_ERROR_HANDLER );

        try( InputStream in = Files.newInputStream( path ) )
        {
            return db.parse( in, path.toAbsolutePath().toString() );
        }
    }

    public static List<Element> getChildElements( Element parent )
    {
        List<Element> elements = new ArrayList<>();

        NodeList childNodes = parent.getChildNodes();
        for( int i = 0; i < childNodes.getLength(); i++ )
        {
            Node childNode = childNodes.item( i );
            if( childNode.getNodeType() == Node.ELEMENT_NODE )
            {
                Element childElement = ( Element ) childNode;
                elements.add( childElement );
            }
        }

        return elements;
    }

    public static List<Element> getChildElements( Element parent, String... localChildrenName )
    {
        List<Element> elements = getChildElements( parent );
        List<String> elementNamesFilter = new ArrayList<>( Arrays.asList( localChildrenName ) );

        Iterator<Element> iterator = elements.iterator();
        while( iterator.hasNext() )
        {
            Element element = iterator.next();
            if( !elementNamesFilter.contains( element.getTagName() ) &&
                !elementNamesFilter.contains( element.getLocalName() ) )
            {
                iterator.remove();
            }
        }

        return elements;
    }

    public static Element getFirstChildElement( Element parent, String localChildName )
    {
        List<Element> elements = getChildElements( parent );

        for( Element childElement : elements )
        {
            if( localChildName.equals( childElement.getTagName() ) || localChildName.equals( childElement.getLocalName() ) )
            {
                return childElement;
            }
        }

        return null;
    }

    public static String getFirstChildElementTextContent( Element parent, String localChildName )
    {
        Element child = getFirstChildElement( parent, localChildName );
        return child == null ? null : child.getTextContent();
    }

    private static class StrictErrorHandler implements ErrorHandler
    {
        public void warning( SAXParseException exception ) throws SAXException
        {
            throw exception;
        }

        public void error( SAXParseException exception ) throws SAXException
        {
            throw exception;
        }

        public void fatalError( SAXParseException exception ) throws SAXException
        {
            throw exception;
        }
    }
}
