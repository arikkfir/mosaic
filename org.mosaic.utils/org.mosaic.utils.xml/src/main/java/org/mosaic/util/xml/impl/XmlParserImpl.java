package org.mosaic.util.xml.impl;

import com.google.common.base.Optional;
import com.google.common.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import javax.xml.xpath.*;
import org.mosaic.util.xml.StrictErrorHandler;
import org.mosaic.util.xml.XmlDocument;
import org.mosaic.util.xml.XmlElement;
import org.mosaic.util.xml.XmlParser;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import static java.util.Collections.emptyList;
import static org.mosaic.util.xml.impl.ConversionServiceTracker.conversionService;

/**
 * @author arik
 */
final class XmlParserImpl implements XmlParser
{
    @Nonnull
    private static final String USER_DATA_XMLELEMENT_KEY = XmlDocumentImpl.XmlElementImpl.class.getName();

    @Nonnull
    @Override
    public XmlDocument parse( @Nonnull Path file ) throws ParserConfigurationException, IOException, SAXException
    {
        try( InputStream is = Files.newInputStream( file, StandardOpenOption.READ ) )
        {
            return parse( file.toUri().toString(), is );
        }
    }

    @Nonnull
    @Override
    public XmlDocument parse( @Nonnull Path file, @Nonnull Schema schema )
            throws ParserConfigurationException, IOException, SAXException
    {
        try( InputStream is = Files.newInputStream( file, StandardOpenOption.READ ) )
        {
            return parse( file.toUri().toString(), schema, is );
        }
    }

    @Nonnull
    @Override
    public XmlDocument parse( @Nonnull String systemId, @Nonnull InputStream is )
            throws ParserConfigurationException, IOException, SAXException
    {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware( true );

        DocumentBuilder db = dbf.newDocumentBuilder();
        db.setErrorHandler( StrictErrorHandler.INSTANCE );
        return new XmlDocumentImpl( db.parse( is, systemId ) );
    }

    @Nonnull
    @Override
    public XmlDocument parse( @Nonnull String systemId,
                              @Nonnull Schema schema,
                              @Nonnull InputStream is ) throws ParserConfigurationException, IOException, SAXException
    {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware( true );
        dbf.setSchema( schema );

        DocumentBuilder db = dbf.newDocumentBuilder();
        db.setErrorHandler( StrictErrorHandler.INSTANCE );
        return new XmlDocumentImpl( db.parse( is, systemId ) );
    }

    private class XmlDocumentImpl implements XmlDocument, NamespaceContext
    {
        @Nonnull
        private final Map<String, String> namespaces = new ConcurrentHashMap<>( 5 );

        @Nonnull
        private final Document document;

        @Nonnull
        private final XmlElement rootElement;

        private XmlDocumentImpl( @Nonnull Document document )
        {
            this.namespaces.put( XMLConstants.XML_NS_PREFIX, XMLConstants.XML_NS_URI );
            this.namespaces.put( XMLConstants.XMLNS_ATTRIBUTE, XMLConstants.XMLNS_ATTRIBUTE_NS_URI );
            this.document = document;
            this.rootElement = new XmlElementImpl( this.document.getDocumentElement() );
        }

        @Override
        public void addNamespace( @Nonnull String prefix, @Nonnull String uri )
        {
            this.namespaces.put( prefix, uri );
        }

        @Nonnull
        @Override
        public XmlElement getRoot()
        {
            return this.rootElement;
        }

        @Nonnull
        @Override
        public String getNamespaceURI( String prefix )
        {
            if( prefix == null )
            {
                throw new NullPointerException();
            }

            String uri = this.namespaces.get( prefix );
            if( uri != null )
            {
                return uri;
            }
            else
            {
                return XMLConstants.NULL_NS_URI;
            }
        }

        @Nonnull
        @Override
        public String getPrefix( String namespaceURI )
        {
            throw new UnsupportedOperationException();
        }

        @Nonnull
        @Override
        public Iterator getPrefixes( String namespaceURI )
        {
            throw new UnsupportedOperationException();
        }

        @Nonnull
        private XPathExpression getXPathExpression( String xpath ) throws XPathException
        {
            XPathFactory xpf = XPathFactory.newInstance();
            XPath xp = xpf.newXPath();
            xp.setNamespaceContext( this );
            return xp.compile( xpath );
        }

        private class XmlElementImpl implements XmlElement
        {
            @Nonnull
            private final Element element;

            private XmlElementImpl( @Nonnull Element element )
            {
                this.element = element;
                this.element.setUserData( USER_DATA_XMLELEMENT_KEY, this, null );
            }

            @Nonnull
            @Override
            public String getName()
            {
                return this.element.getLocalName();
            }

            @Override
            @Nonnull
            public Optional<String> getValue()
            {
                return Optional.fromNullable( this.element.getTextContent() );
            }

            @Override
            @Nonnull
            public <T> Optional<T> getValue( @Nonnull TypeToken<T> type )
            {
                Optional<String> value = getValue();
                return value.isPresent() ? Optional.fromNullable( conversionService().convert( value.get(), type ) ) : Optional.<T>absent();
            }

            @Override
            @Nonnull
            public Optional<String> getAttribute( @Nonnull String name )
            {
                Attr attr = this.element.getAttributeNode( name );
                return attr == null ? Optional.<String>absent() : Optional.fromNullable( attr.getValue() );
            }

            @Override
            @Nonnull
            public <T> Optional<T> getAttribute( @Nonnull String name, @Nonnull TypeToken<T> type )
            {
                Optional<String> value = getAttribute( name );
                return value.isPresent() ? Optional.fromNullable( conversionService().convert( value.get(), type ) ) : Optional.<T>absent();
            }

            @Override
            @Nonnull
            public <T> Optional<T> find( @Nonnull String xpath, @Nonnull TypeToken<T> type ) throws XPathException
            {
                XPathExpression expr = getXPathExpression( xpath );

                Node node = ( Node ) expr.evaluate( this.element, XPathConstants.NODE );
                return node == null ? Optional.<T>absent() : Optional.fromNullable( conversionService().convert( node.getTextContent(), type ) );
            }

            @Override
            @Nonnull
            public Optional<XmlElement> getFirstChildElement()
            {
                NodeList nodes = this.element.getChildNodes();
                for( int i = 0; i < nodes.getLength(); i++ )
                {
                    Node node = nodes.item( i );
                    if( node instanceof Element )
                    {
                        return Optional.of( getXmlElementFromNode( node ) );
                    }
                }
                return Optional.absent();
            }

            @Override
            @Nonnull
            public Optional<XmlElement> getFirstChildElement( @Nonnull String name )
            {
                NodeList nodes = this.element.getChildNodes();
                for( int i = 0; i < nodes.getLength(); i++ )
                {
                    Node node = nodes.item( i );
                    if( node instanceof Element )
                    {
                        Element element = ( Element ) node;
                        if( element.getLocalName().equals( name ) )
                        {
                            return Optional.of( getXmlElementFromNode( node ) );
                        }
                    }
                }
                return Optional.absent();
            }

            @Override
            @Nonnull
            public List<XmlElement> getChildElements()
            {
                return getXmlElementsFromNodeList( this.element.getChildNodes() );
            }

            @Override
            @Nonnull
            public List<XmlElement> getChildElements( @Nonnull String name )
            {
                NodeList nodes = this.element.getChildNodes();
                List<XmlElement> children = null;
                for( int i = 0; i < nodes.getLength(); i++ )
                {
                    Node node = nodes.item( i );
                    if( node instanceof Element )
                    {
                        Element element = ( Element ) node;
                        if( element.getLocalName().equals( name ) )
                        {
                            if( children == null )
                            {
                                children = new LinkedList<>();
                            }
                            children.add( getXmlElementFromNode( node ) );
                        }
                    }
                }
                return children == null ? Collections.<XmlElement>emptyList() : Collections.unmodifiableList( children );
            }

            @Override
            @Nonnull
            public List<String> findTexts( @Nonnull String xpath ) throws XPathException
            {
                XPathExpression expr = getXPathExpression( xpath );
                NodeList nodes = ( NodeList ) expr.evaluate( this.element, XPathConstants.NODESET );
                if( nodes.getLength() > 0 )
                {
                    List<String> values = null;
                    for( int i = 0; i < nodes.getLength(); i++ )
                    {
                        Node node = nodes.item( i );
                        String textContent = node.getTextContent();
                        if( textContent != null )
                        {
                            textContent = textContent.trim();
                            if( !textContent.isEmpty() )
                            {
                                if( values == null )
                                {
                                    values = new LinkedList<>();
                                }
                                values.add( textContent );
                            }
                        }
                    }
                    return values == null ? Collections.<String>emptyList() : values;
                }
                else
                {
                    return emptyList();
                }
            }

            @Override
            @Nonnull
            public Optional<XmlElement> findFirstElement( @Nonnull String xpath ) throws XPathException
            {
                XPathExpression expr = getXPathExpression( xpath );
                NodeList nodes = ( NodeList ) expr.evaluate( this.element, XPathConstants.NODESET );
                for( int i = 0; i < nodes.getLength(); i++ )
                {
                    Node node = nodes.item( i );
                    if( node instanceof Element )
                    {
                        return Optional.of( getXmlElementFromNode( node ) );
                    }
                }
                return Optional.absent();
            }

            @Override
            @Nonnull
            public List<XmlElement> findElements( @Nonnull String xpath ) throws XPathException
            {
                XPathExpression expr = getXPathExpression( xpath );
                NodeList nodes = ( NodeList ) expr.evaluate( this.element, XPathConstants.NODESET );
                return getXmlElementsFromNodeList( nodes );
            }

            @Nonnull
            private List<XmlElement> getXmlElementsFromNodeList( @Nonnull NodeList nodeList )
            {
                List<XmlElement> children = null;
                for( int i = 0; i < nodeList.getLength(); i++ )
                {
                    Node node = nodeList.item( i );
                    if( node instanceof Element )
                    {
                        if( children == null )
                        {
                            children = new LinkedList<>();
                        }
                        children.add( getXmlElementFromNode( node ) );
                    }
                }
                return children == null ? Collections.<XmlElement>emptyList() : Collections.unmodifiableList( children );
            }

            @Nonnull
            private XmlElement getXmlElementFromNode( @Nonnull Node node )
            {
                XmlElementImpl xmlElement = ( XmlElementImpl ) node.getUserData( USER_DATA_XMLELEMENT_KEY );
                if( xmlElement == null )
                {
                    xmlElement = new XmlElementImpl( ( Element ) node );
                }
                return xmlElement;
            }
        }
    }
}
