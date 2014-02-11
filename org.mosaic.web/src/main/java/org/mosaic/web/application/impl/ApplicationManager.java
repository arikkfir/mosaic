package org.mosaic.web.application.impl;

import com.google.common.base.Optional;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatterBuilder;
import org.mosaic.event.EventListener;
import org.mosaic.modules.Component;
import org.mosaic.modules.ModuleEvent;
import org.mosaic.modules.ModuleEventType;
import org.mosaic.modules.Service;
import org.mosaic.pathwatchers.OnPathCreated;
import org.mosaic.pathwatchers.OnPathDeleted;
import org.mosaic.pathwatchers.OnPathModified;
import org.mosaic.server.Server;
import org.mosaic.util.xml.StrictErrorHandler;
import org.mosaic.util.xml.XmlElement;
import org.mosaic.util.xml.XmlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import static java.nio.file.Files.notExists;

/**
 * @author arik
 */
@Component
final class ApplicationManager
{
    private static final Logger LOG = LoggerFactory.getLogger( ApplicationManager.class );

    private static final String APPS_FILE_PATTERN = "${mosaic.home.apps}/**/*.xml";

    @Nonnull
    static Period parsePeriod( @Nonnull String period )
    {
        PeriodFormatterBuilder builder = new PeriodFormatterBuilder().printZeroNever();
        if( period.contains( "year" ) )
        {
            builder.appendYears()
                   .appendSuffix( " year", " years" )
                   .appendSeparatorIfFieldsAfter( ", " );
        }
        if( period.contains( "month" ) )
        {
            builder.appendMonths()
                   .appendSuffix( " month", " months" )
                   .appendSeparatorIfFieldsAfter( ", " );
        }
        if( period.contains( "day" ) )
        {
            builder.appendDays()
                   .appendSuffix( " day", " days" )
                   .appendSeparatorIfFieldsAfter( ", " );
        }
        if( period.contains( "hour" ) )
        {
            builder.appendHours()
                   .appendSuffix( " hour", " hours" )
                   .appendSeparatorIfFieldsAfter( ", " );
        }
        if( period.contains( "minute" ) )
        {
            builder.appendMinutes()
                   .appendSuffix( " minute", " minutes" )
                   .appendSeparatorIfFieldsAfter( ", " );
        }
        if( period.contains( "second" ) )
        {
            builder.appendSeconds()
                   .appendSuffix( " second", " seconds" );
        }
        return builder.printZeroRarelyLast().toFormatter().parsePeriod( period ).normalizedStandard();
    }

    @Nonnull
    private final Map<String, ApplicationHolder> applications = new ConcurrentHashMap<>();

    @Nonnull
    private final Schema applicationSchema;

    @Nonnull
    private final Schema applicationFragmentSchema;

    @Nonnull
    @Service
    private Server server;

    @Nonnull
    @Service
    private XmlParser xmlParser;

    ApplicationManager() throws IOException, SAXException
    {

        Path appSchemaFile = this.server.getHome().resolve( "schemas/application-1.0.0.xsd" );
        if( notExists( appSchemaFile ) )
        {
            throw new IllegalStateException( "could not find application schema at '" + appSchemaFile + "'" );
        }

        Path appFragmentSchemaFile = this.server.getHome().resolve( "schemas/application-fragment-1.0.0.xsd" );
        if( notExists( appFragmentSchemaFile ) )
        {
            throw new IllegalStateException( "could not find application fragment schema at '" + appFragmentSchemaFile + "'" );
        }

        SchemaFactory schemaFactory = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
        schemaFactory.setErrorHandler( StrictErrorHandler.INSTANCE );
        try( InputStream stream100 = Files.newInputStream( appSchemaFile ) )
        {
            this.applicationSchema = schemaFactory.newSchema( new Source[] {
                    new StreamSource( stream100, "http://www.mosaicserver.com/application-1.0.0" )
            } );
        }
        try( InputStream stream100 = Files.newInputStream( appFragmentSchemaFile ) )
        {
            this.applicationFragmentSchema = schemaFactory.newSchema( new Source[] {
                    new StreamSource( stream100, "http://www.mosaicserver.com/application-fragment-1.0.0" )
            } );
        }
    }

    @EventListener
    void onModuleActivationChanged( @Nonnull ModuleEvent event )
    {
        ModuleEventType eventType = event.getEventType();
        if( eventType == ModuleEventType.ACTIVATED || eventType == ModuleEventType.DEACTIVATING )
        {
            Optional<Path> resourceHolder;
            try
            {
                resourceHolder = event.getModule().findResource( "/WEB-INF/application.xml" );
            }
            catch( IOException e )
            {
                LOG.error( "Could not inspect module '{}': {}", event.getModule(), e.getMessage(), e );
                return;
            }

            if( resourceHolder.isPresent() )
            {
                Path file = resourceHolder.get();

                XmlElement appElt;
                try
                {
                    appElt = this.xmlParser.parse( file, this.applicationFragmentSchema ).getRoot();
                }
                catch( Throwable e )
                {
                    LOG.error( "Could not parse application fragment at '{}': {}", file, e.getMessage(), e );
                    return;
                }

                Optional<String> idHolder = appElt.getAttribute( "id" );
                if( idHolder.isPresent() )
                {
                    String id = idHolder.get();

                    ApplicationHolder application = this.applications.get( id );
                    if( eventType == ModuleEventType.ACTIVATED )
                    {
                        if( application == null )
                        {
                            application = new ApplicationHolder( id, this.applicationSchema, this.applicationFragmentSchema );
                            this.applications.put( id, application );
                        }
                        application.addContributionFile( file );
                    }
                    else
                    {
                        if( application != null )
                        {
                            application.removeContributionFile( file );
                        }
                    }
                }
            }
        }
    }

    @OnPathCreated(APPS_FILE_PATTERN)
    @OnPathModified(APPS_FILE_PATTERN)
    void onApplicationAddedOrModifiedInEtc( @Nonnull Path file )
    {
        String fileName = file.getFileName().toString();
        String id = fileName.substring( 0, fileName.length() - ".xml".length() );

        ApplicationHolder application = this.applications.get( id );
        if( application == null )
        {
            application = new ApplicationHolder( id, this.applicationSchema, this.applicationFragmentSchema );
            this.applications.put( id, application );
        }
        application.addApplicationFile( file );
    }

    @OnPathDeleted(APPS_FILE_PATTERN)
    void onApplicationDeletedInEtc( @Nonnull Path file )
    {
        String fileName = file.getFileName().toString();
        String id = fileName.substring( 0, fileName.length() - ".xml".length() );

        ApplicationHolder application = this.applications.get( id );
        if( application != null )
        {
            try
            {
                application.removeApplicationFile( file );
            }
            catch( Throwable ignore )
            {
            }
        }
    }
}
