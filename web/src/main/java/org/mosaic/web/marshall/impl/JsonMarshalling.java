package org.mosaic.web.marshall.impl;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.reflect.TypeToken;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.lifecycle.annotation.Bean;
import org.mosaic.web.marshall.annotation.Marshaller;
import org.mosaic.web.marshall.annotation.Unmarshaller;
import org.mosaic.web.marshall.annotation.Value;

/**
 * @author arik
 */
@Bean
public class JsonMarshalling
{
    @Nonnull
    private ObjectMapper objectMapper;

    public JsonMarshalling()
    {
        JsonFactory jsonFactory = new JsonFactory();
        jsonFactory.enable( JsonParser.Feature.ALLOW_COMMENTS );
        jsonFactory.enable( JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER );
        jsonFactory.enable( JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS );
        jsonFactory.enable( JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS );
        jsonFactory.enable( JsonParser.Feature.ALLOW_SINGLE_QUOTES );
        jsonFactory.enable( JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES );
        jsonFactory.disable( JsonParser.Feature.AUTO_CLOSE_SOURCE );

        ObjectMapper mapper = new ObjectMapper( jsonFactory );
        mapper.enable( DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY );
        mapper.enable( DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES );
        mapper.enable( DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS );
        mapper.enable( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES );
        mapper.enable( SerializationFeature.INDENT_OUTPUT );
        mapper.disable( SerializationFeature.CLOSE_CLOSEABLE );
        mapper.disable( SerializationFeature.FLUSH_AFTER_WRITE_VALUE );
        mapper.enable( SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN );
        mapper.disable( MapperFeature.USE_GETTERS_AS_SETTERS );
        this.objectMapper = mapper;
    }

    @Marshaller( produces = "application/json" )
    public void marshall( @Value @Nullable Object value, @Nonnull Writer out ) throws IOException
    {
        this.objectMapper.writeValue( out, value );
    }

    @Unmarshaller( consumes = "application/json" )
    public Object unmarshall( @Nonnull Reader in, @Nonnull TypeToken<?> type ) throws IOException
    {
        return this.objectMapper.readValue( in, type.getRawType() );
    }
}
