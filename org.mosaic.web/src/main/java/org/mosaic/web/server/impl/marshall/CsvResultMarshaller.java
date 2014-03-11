package org.mosaic.web.server.impl.marshall;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.common.net.MediaType;
import javax.annotation.Nonnull;
import org.mosaic.modules.Ranking;
import org.mosaic.modules.Service;
import org.mosaic.web.server.HandlerResultMarshaller;
import org.mosaic.web.server.WebInvocation;

/**
 * @author arik
 */
@Service
@Ranking(-200)
final class CsvResultMarshaller implements HandlerResultMarshaller
{
    private static final MediaType APPLICATION_CSV = MediaType.create( "application", "csv" );

    @Nonnull
    private final ObjectMapper objectMapper;

    CsvResultMarshaller()
    {
        CsvMapper objectMapper = new CsvMapper();
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
        objectMapper.configure( CsvParser.Feature.WRAP_AS_ARRAY, true );
        objectMapper.registerModules(
                new GuavaModule(),
                new JodaModule()
        );
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean canMarshall( @Nonnull MediaType mediaType, @Nonnull Object value )
    {
        return APPLICATION_CSV.is( mediaType );
    }

    @Override
    public void marshall( @Nonnull WebInvocation invocation, @Nonnull Object value ) throws Exception
    {
        invocation.getHttpResponse().setContentType( APPLICATION_CSV );
        this.objectMapper.writeValue( invocation.getHttpResponse().getOutputStream(), value );
    }
}
