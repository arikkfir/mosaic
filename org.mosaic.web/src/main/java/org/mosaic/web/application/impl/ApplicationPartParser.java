package org.mosaic.web.application.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.Nonnull;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.mosaic.modules.Component;
import org.mosaic.modules.Service;
import org.mosaic.server.Server;
import org.mosaic.util.xml.StrictErrorHandler;
import org.mosaic.util.xml.XmlDocument;
import org.mosaic.util.xml.XmlElement;
import org.mosaic.util.xml.XmlParser;
import org.xml.sax.SAXException;

/**
 * @author arik
 */
@Component
final class ApplicationPartParser
{
    @Nonnull
    private final Schema applicationSchema;

    @Nonnull
    @Service
    private Server server;

    @Nonnull
    @Service
    private XmlParser xmlParser;

    ApplicationPartParser() throws IOException, SAXException
    {
        Path schemaFile = this.server.getHome().resolve( "schemas/application-1.0.0.xsd" );
        if( Files.notExists( schemaFile ) )
        {
            throw new IllegalStateException( "could not find permission policy schema at '" + schemaFile + "'" );
        }

        SchemaFactory schemaFactory = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
        schemaFactory.setErrorHandler( StrictErrorHandler.INSTANCE );
        try( InputStream stream100 = Files.newInputStream( schemaFile ) )
        {
            this.applicationSchema = schemaFactory.newSchema( new Source[] {
                    new StreamSource( stream100, "http://www.mosaicserver.com/application-1.0.0" )
            } );
        }
    }

    @Nonnull
    XmlElement parse( @Nonnull Path file ) throws IOException, SAXException, ParserConfigurationException
    {
        XmlDocument doc = this.xmlParser.parse( file, this.applicationSchema );
        doc.addNamespace( "m", "http://www.mosaicserver.com/application-1.0.0" );
        return doc.getRoot();
    }

    @Nonnull
    XmlElement parse( @Nonnull URL url ) throws IOException, SAXException, ParserConfigurationException
    {
        try( InputStream inputStream = url.openStream() )
        {
            XmlDocument doc = this.xmlParser.parse( inputStream, url.toExternalForm(), this.applicationSchema );
            doc.addNamespace( "m", "http://www.mosaicserver.com/application-1.0.0" );
            return doc.getRoot();
        }
    }
}
