package org.mosaic.web.request.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.mosaic.web.request.HttpStatus;
import org.mosaic.web.request.WebResponse;
import org.mosaic.web.request.WebResponseHeaders;

/**
 * @author arik
 */
final class WebResponseImpl implements WebResponse
{
    @Nonnull
    private final Response response;

    @Nonnull
    private final WebResponseHeaders headers;

    WebResponseImpl( @Nonnull Request request )
    {
        this.response = request.getResponse();
        this.headers = new WebResponseHeadersImpl( request, this.response );
    }

    @Override
    public boolean isCommitted()
    {
        return this.response.isCommitted();
    }

    @Nullable
    @Override
    public HttpStatus getStatus()
    {
        return HttpStatus.valueOf( this.response.getStatus() );
    }

    @Override
    public void setStatus( @Nonnull HttpStatus status )
    {
        this.response.setStatus( status.value() );
    }

    @Override
    public void setStatus( @Nonnull HttpStatus status, @Nullable String text )
    {
        this.response.setStatusWithReason( status.value(), text );
    }

    @Override
    public void reset()
    {
        this.response.reset();
    }

    @Nonnull
    @Override
    public OutputStream stream() throws IOException
    {
        return this.response.getOutputStream();
    }

    @Nonnull
    @Override
    public Writer writer() throws IOException
    {
        return this.response.getWriter();
    }

    @Override
    public void addCookie( @Nonnull String name, @Nonnull Object value )
    {
        this.response.getHttpFields().addSetCookie( name, value.toString(), null, null, -1, null, false, false, 1 );
    }

    @Override
    public void addCookie( @Nonnull String name, @Nonnull Object value, @Nonnull String domain )
    {
        this.response.getHttpFields().addSetCookie( name, value.toString(), domain, null, -1, null, false, false, 1 );
    }

    @Override
    public void addCookie( @Nonnull String name, @Nonnull Object value, @Nonnull String domain, @Nonnull String path )
    {
        this.response.getHttpFields().addSetCookie( name, value.toString(), domain, path, -1, null, false, false, 1 );
    }

    @Override
    public void addCookie( @Nonnull String name,
                           @Nonnull Object value,
                           @Nonnull String domain,
                           @Nonnull String path,
                           @Nonnull Period maxAge )
    {
        int seconds = maxAge.toStandardSeconds().getSeconds();
        this.response.getHttpFields().addSetCookie( name, value.toString(), domain, path, seconds, null, false, false, 1 );
    }

    @Override
    public void removeCookie( @Nonnull String name )
    {
        this.response.getHttpFields().addSetCookie( name, null, null, null, 0, null, false, false, 1 );
    }

    @Override
    public void disableCaching()
    {
        this.headers.setCacheControl( "no-cache, no-store, must-revalidate" );
        this.headers.setExpires( new DateTime() );
        this.headers.setPragma( "no-cache" );
    }
}
