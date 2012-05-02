package org.mosaic.server.web.jetty.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import javax.servlet.http.Part;
import org.mosaic.util.collection.TypedDict;
import org.mosaic.util.collection.WrappingTypedDict;
import org.mosaic.web.HttpPart;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.MediaType;

import static java.util.Collections.unmodifiableMap;

/**
 * @author arik
 */
public class HttpPartImpl implements HttpPart
{

    private final Part part;

    private final TypedDict<String> headers;

    public HttpPartImpl( Part part, ConversionService conversionService )
    {
        this.part = part;

        Collection<String> headerNames = this.part.getHeaderNames( );
        Map<String, List<String>> headers = new HashMap<>( headerNames.size( ) );
        for( String headerName : headerNames )
        {
            headers.put( headerName, new LinkedList<>( this.part.getHeaders( headerName ) ) );
        }
        this.headers = new WrappingTypedDict<>( unmodifiableMap( headers ), conversionService, String.class );
    }

    @Override
    public String getName( )
    {
        return this.part.getName( );
    }

    @Override
    public MediaType getContentType( )
    {
        return MediaType.parseMediaType( this.part.getContentType( ) );
    }

    @Override
    public long getSize( )
    {
        return this.part.getSize( );
    }

    @Override
    public InputStream getInputStream( ) throws IOException
    {
        return this.part.getInputStream( );
    }

    @Override
    public void save( Path path ) throws IOException
    {
        this.part.write( path.toString( ) );
    }

    @Override
    public TypedDict<String> getHeaders( )
    {
        return this.headers;
    }
}
