package org.mosaic.web.application.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPathException;
import org.mosaic.modules.Component;
import org.mosaic.modules.Module;
import org.mosaic.modules.Service;
import org.mosaic.pathwatchers.PathWatcher;
import org.mosaic.util.xml.StrictErrorHandler;
import org.mosaic.util.xml.XmlDocument;
import org.mosaic.util.xml.XmlParser;
import org.xml.sax.SAXException;

import static org.mosaic.util.resource.PathEvent.*;

/**
 * @author arik
 */
@Component
final class ApplicationManager
{
    @Nonnull
    private final Schema applicationSchema;

    @Nonnull
    private final Map<String, ApplicationImpl> applications = new ConcurrentHashMap<>();

    @Nonnull
    @Component
    private Module module;

    @Nonnull
    @Service
    private XmlParser xmlParser;

    ApplicationManager() throws IOException, SAXException
    {
        Path schemaFile = this.module.getContext().getServerHome().resolve( "schemas/application-1.0.0.xsd" );
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

    @PathWatcher(value = "${mosaic.home.apps}/**/*.xml", events = { CREATED, MODIFIED })
    void onApplicationAddedOrModified( @Nonnull Path file )
            throws IOException, SAXException, ParserConfigurationException, XPathException
    {
        XmlDocument doc = this.xmlParser.parse( file, this.applicationSchema );
        doc.addNamespace( "m", "http://www.mosaicserver.com/application-1.0.0" );

        String id;
        String fileName = file.getFileName().toString();
        if( fileName.endsWith( ".xml" ) )
        {
            id = fileName.substring( 0, fileName.length() - ".xml".length() );
        }
        else
        {
            id = fileName;
        }

        ApplicationImpl application = this.applications.get( id );
        if( application == null )
        {
            application = new ApplicationImpl( id, doc.getRoot() );
            this.applications.put( id, application );
        }
        else
        {
            application.parse( doc.getRoot() );
        }
    }

    @PathWatcher(value = "${mosaic.home.apps}/**/*.xml", events = DELETED)
    void onApplicationDeleted( @Nonnull Path file )
    {
        String id;
        String fileName = file.getFileName().toString();
        if( fileName.endsWith( ".xml" ) )
        {
            id = fileName.substring( 0, fileName.length() - ".xml".length() );
        }
        else
        {
            id = fileName;
        }

        ApplicationImpl application = this.applications.remove( id );
        if( application != null )
        {
            application.unregister();
        }
    }
}
