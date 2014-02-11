package org.mosaic.web.server;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.slf4j.Logger;

import static com.google.common.base.Strings.padStart;

/**
 * @author arik
 */
public class HttpLogger
{
    @Nonnull
    private final Logger logger;

    @Nonnull
    private final HttpRequest request;

    public HttpLogger( @Nonnull Logger logger, @Nonnull HttpRequest request )
    {
        this.logger = logger;
        this.request = request;
    }

    public void trace( @Nullable String message, @Nullable Object... arguments )
    {
        if( logger.isTraceEnabled() )
        {
            logger.trace( message + "\n" + getDebugString(), arguments );
        }
    }

    public void debug( @Nullable String message, @Nullable Object... arguments )
    {
        if( logger.isDebugEnabled() )
        {
            logger.debug( message + "\n" + getDebugString(), arguments );
        }
    }

    public void info( @Nullable String message, @Nullable Object... arguments )
    {
        if( logger.isInfoEnabled() )
        {
            logger.info( message + "\n" + getDebugString(), arguments );
        }
    }

    public void warn( @Nullable String message, @Nullable Object... arguments )
    {
        if( logger.isWarnEnabled() )
        {
            logger.warn( message + "\n" + getDebugString(), arguments );
        }
    }

    public void error( @Nullable String message, @Nullable Object... arguments )
    {
        if( logger.isErrorEnabled() )
        {
            logger.error( message + "\n" + getDebugString(), arguments );
        }
    }

    @Nonnull
    private String getDebugString()
    {
        HttpRequestUri uri = this.request.getUri();

        StringBuilder buffer = new StringBuilder( 5000 );
        buffer.append( "\n" );
        buffer.append( "GENERAL INFORMATION\n" );
        buffer.append( "              Client address: " ).append( this.request.getClientAddress() ).append( "\n" );
        buffer.append( "                      Method: " ).append( this.request.getMethod() ).append( "\n" );
        buffer.append( "                   Jetty URL: " ).append( uri ).append( "\n" );
        buffer.append( "                      Scheme: " ).append( uri.getScheme() ).append( "\n" );
        buffer.append( "                        Host: " ).append( uri.getHost() ).append( "\n" );
        buffer.append( "                        Port: " ).append( uri.getPort() ).append( "\n" );
        buffer.append( "                Decoded path: " ).append( uri.getDecodedPath() ).append( "\n" );
        buffer.append( "                Encoded path: " ).append( uri.getEncodedPath() ).append( "\n" );
        buffer.append( "               Encoded query: " ).append( uri.getEncodedQueryString() ).append( "\n" );
        buffer.append( "                    Fragment: " ).append( uri.getFragment() ).append( "\n" );
        buffer.append( "\n" );
        buffer.append( "REQUEST HEADERS\n" );

        for( String headerName : this.request.getHeaderNames() )
        {
            List<String> values = this.request.getHeader( headerName );

            headerName = padStart( headerName, 20, ' ' );
            buffer.append( "        " ).append( padStart( headerName, 20, ' ' ) ).append( ": " );

            if( values.isEmpty() )
            {
                buffer.append( "\n" );
            }
            else
            {
                boolean first = true;
                for( String value : values )
                {
                    if( first )
                    {
                        first = false;
                    }
                    else
                    {
                        buffer.append( ", " );
                    }
                    buffer.append( value ).append( "\n" );
                }
            }
        }
        return buffer.toString().replace( "{}", "\\{}" );
    }
}
