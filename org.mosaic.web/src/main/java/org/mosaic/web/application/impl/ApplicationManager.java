package org.mosaic.web.application.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.mosaic.event.EventListener;
import org.mosaic.modules.*;
import org.mosaic.pathwatchers.PathWatcher;
import org.mosaic.server.Server;
import org.mosaic.util.xml.StrictErrorHandler;
import org.mosaic.util.xml.XmlDocument;
import org.mosaic.util.xml.XmlElement;
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
    private final Map<String, Set<XmlElement>> moduleApplicationFiles = new ConcurrentHashMap<>();

    @Nonnull
    private final Map<String, ApplicationImpl> applications = new ConcurrentHashMap<>();

    @Nonnull
    @Service
    private Server server;

    @Nonnull
    @Service
    private XmlParser xmlParser;

    ApplicationManager() throws IOException, SAXException
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

    @EventListener
    void onModuleActivationChanged( @Nonnull ModuleEvent event )
    {
        // TODO arik: implement org.mosaic.web.application.impl.ApplicationManager.onModuleActivationChanged([event])
        if( event.getEventType() == ModuleEventType.ACTIVATED )
        {
            Module module = event.getModule();
            module.getModuleResources().findResources( "/META-INF/" );
        }
        else if( event.getEventType() == ModuleEventType.DEACTIVATING )
        {

        }
    }

    @PathWatcher( value = "${mosaic.home.apps}/**/*.xml", events = { CREATED, MODIFIED } )
    void onApplicationAddedOrModifiedInEtc( @Nonnull Path file ) throws Exception
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

    @PathWatcher( value = "${mosaic.home.apps}/**/*.xml", events = DELETED )
    void onApplicationDeletedInEtc( @Nonnull Path file )
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
