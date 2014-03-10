package org.mosaic.web.server.impl.marshall;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.common.net.MediaType;
import javax.annotation.Nonnull;
import org.mosaic.modules.Service;
import org.mosaic.web.server.MessageMarshaller;

/**
 * @author arik
 */
@Service
final class JsonMessageMarshaller implements MessageMarshaller
{
    private static final MediaType APPLICATION_JSON = MediaType.create( "application", "json" );

    @Nonnull
    private final ObjectMapper objectMapper;

    JsonMessageMarshaller()
    {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure( MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true );
        objectMapper.configure( SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true );
        objectMapper.configure( SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN, true );
        objectMapper.configure( SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false );
        objectMapper.configure( DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true );
        objectMapper.configure( DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true );
        objectMapper.configure( DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS, true );
        objectMapper.configure( DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY, true );
        objectMapper.configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true );
        objectMapper.configure( DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false );
        objectMapper.configure( DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true );
        objectMapper.configure( JsonParser.Feature.ALLOW_COMMENTS, true );
        objectMapper.configure( JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS, true );
        objectMapper.configure( JsonParser.Feature.ALLOW_SINGLE_QUOTES, true );
        objectMapper.configure( JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true );
        objectMapper.configure( JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true );
        objectMapper.configure( JsonParser.Feature.STRICT_DUPLICATE_DETECTION, true );
        objectMapper.configure( JsonGenerator.Feature.AUTO_CLOSE_TARGET, false );
        objectMapper.configure( JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true );
        objectMapper.registerModules(
                new GuavaModule(),
                new JodaModule()
        );
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean canMarshall( @Nonnull Object value, @Nonnull MediaType mediaType )
    {
        return APPLICATION_JSON.is( mediaType );
    }

    @Override
    public void marshall( @Nonnull MarshallingSink sink ) throws Exception
    {
        sink.setContentType( APPLICATION_JSON );
        this.objectMapper.writeValue( sink.getOutputStream(), sink.getValue() );
    }
}
