package org.mosaic.server.web.dispatcher.impl.marshall;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Type;
import javax.annotation.PostConstruct;
import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.deser.std.StdDeserializer;
import org.codehaus.jackson.map.module.SimpleModule;
import org.codehaus.jackson.map.ser.std.SerializerBase;
import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.mosaic.lifecycle.ContextRef;
import org.mosaic.web.HttpRequest;
import org.mosaic.web.handler.Marshaller;
import org.osgi.framework.BundleContext;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import static org.mosaic.server.web.util.HttpTime.ZONE;

/**
 * @author arik
 */
@Component
public class JsonMarshaller implements Marshaller
{
    private BundleContext bundleContext;

    private ObjectMapper objectMapper = new ObjectMapper();

    @ContextRef
    public void setBundleContext( BundleContext bundleContext )
    {
        this.bundleContext = bundleContext;
    }

    @PostConstruct
    public void init()
    {
        org.osgi.framework.Version bundleVersion = this.bundleContext.getBundle().getVersion();
        int major = bundleVersion.getMajor();
        int minor = bundleVersion.getMinor();
        int micro = bundleVersion.getMicro();
        String qualifier = bundleVersion.getQualifier();

        this.objectMapper.configure( DeserializationConfig.Feature.FAIL_ON_NULL_FOR_PRIMITIVES, true );
        this.objectMapper.configure( DeserializationConfig.Feature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, false );
        this.objectMapper.configure( DeserializationConfig.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY, false );
        this.objectMapper.configure( DeserializationConfig.Feature.FAIL_ON_NUMBERS_FOR_ENUMS, true );
        this.objectMapper.configure( DeserializationConfig.Feature.USE_BIG_DECIMAL_FOR_FLOATS, true );

        SimpleModule module = new SimpleModule( getClass().getPackage().getName(),
                                                new Version( major, minor, micro, qualifier ) );

        module.addDeserializer( MediaType.class, new StdDeserializer<MediaType>( MediaType.class )
        {
            @Override
            public MediaType deserialize( JsonParser jp, DeserializationContext ctxt ) throws IOException
            {
                return MediaType.parseMediaType( jp.getText() );
            }
        }
        );

        module.addDeserializer( DateTime.class, new StdDeserializer<DateTime>( DateTime.class )
        {
            @Override
            public DateTime deserialize( JsonParser jp, DeserializationContext ctxt ) throws IOException
            {
                return new DateTime( jp.getLongValue() ).withZone( ZONE );
            }
        }
        );
        module.addSerializer( DateTime.class, new SerializerBase<DateTime>( DateTime.class )
        {
            @Override
            public void serialize( DateTime value, JsonGenerator jgen, SerializerProvider provider ) throws IOException
            {
                jgen.writeNumber( new DateTime( value.toDateTime() ).withZone( ZONE ).getMillis() );
            }

            @Override
            public JsonNode getSchema( SerializerProvider provider, Type typeHint ) throws JsonMappingException
            {
                return createSchemaNode( "number", true );
            }
        }
        );
        module.addDeserializer( DateMidnight.class, new StdDeserializer<DateMidnight>( DateMidnight.class )
        {
            @Override
            public DateMidnight deserialize( JsonParser jp, DeserializationContext ctxt ) throws IOException
            {
                return new DateMidnight( jp.getLongValue() ).withZoneRetainFields( ZONE );
            }
        }
        );
        module.addSerializer( DateMidnight.class, new SerializerBase<DateMidnight>( DateMidnight.class )
        {
            @Override
            public void serialize( DateMidnight value, JsonGenerator jgen, SerializerProvider provider )
            throws IOException
            {
                jgen.writeNumber( value.withZoneRetainFields( ZONE ).getMillis() );
            }

            @Override
            public JsonNode getSchema( SerializerProvider provider, Type typeHint ) throws JsonMappingException
            {
                return createSchemaNode( "number", true );
            }
        }
        );
        this.objectMapper.registerModule( module );
    }

    @Override
    public boolean matches( HttpRequest request, Object handlerResult )
    {
        for( MediaType mediaType : request.getRequestHeaders().getAccept() )
        {
            if( MediaType.APPLICATION_JSON.includes( mediaType ) )
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public Object marshall( HttpRequest request, Object handlerResult ) throws Exception
    {
        JsonFactory jsonFactory = this.objectMapper.getJsonFactory();
        request.getResponseHeaders().setContentType( MediaType.APPLICATION_JSON );
        Writer writer = request.getResponseWriter();

        //
        // JSONP prefix
        //
        String jp = request.getQueryParameters().getFirst( "jp" );
        if( jp != null )
        {
            writer.write( jp + "(" );
        }

        //
        // actual JSON
        //
        JsonGenerator generator = jsonFactory.createJsonGenerator( writer ).useDefaultPrettyPrinter();
        this.objectMapper.writeValue( generator, handlerResult );

        //
        // JSONP suffix
        //
        if( jp != null )
        {
            writer.write( ")" );
        }
        return null;
    }
}
