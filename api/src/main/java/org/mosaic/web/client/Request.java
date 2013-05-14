package org.mosaic.web.client;

import com.google.common.reflect.TypeToken;
import java.net.URI;
import javax.annotation.Nonnull;
import org.mosaic.web.net.MediaType;
import org.mosaic.web.request.WebRequest;

/**
 * @author arik
 */
public interface Request
{
    @Nonnull
    Request withEncodedBaseUrl( @Nonnull String baseUrl );

    @Nonnull
    Request withScheme( @Nonnull String scheme );

    @Nonnull
    Request withHost( @Nonnull String host );

    @Nonnull
    Request withPort( int port );

    @Nonnull
    Request withHostAndPort( @Nonnull String host, int port );

    @Nonnull
    Request withMethod( @Nonnull String method );

    @Nonnull
    Request withDecodedPath( @Nonnull String path );

    @Nonnull
    Request withPathParameter( String name, Object value );

    @Nonnull
    Request withHeader( @Nonnull String headerName, @Nonnull Object... values );

    @Nonnull
    Request withBasicAuthentication( @Nonnull String username, @Nonnull String password );

    @Nonnull
    Request withAccept( @Nonnull String mediaType );

    @Nonnull
    Request withUserAgent( @Nonnull String userAgent );

    @Nonnull
    Request withQueryParameter( @Nonnull String name );

    @Nonnull
    Request withQueryParameter( @Nonnull String name, @Nonnull Object value );

    @Nonnull
    Request withEncodedQueryString( @Nonnull String query );

    @Nonnull
    Request clearQueryParameters();

    @Nonnull
    Request withPayload( @Nonnull MediaType contentType, @Nonnull Object payload );

    @Nonnull
    Request copyFrom( @Nonnull WebRequest source ) throws ClientRequestBuildException;

    @Nonnull
    URI getUri();

    @Nonnull
    <Type> Response<Type> invokeAndUnmarshall( @Nonnull TypeToken<Type> responseType )
            throws InterruptedException,
                   ClientRequestConnectException,
                   ClientRequestSendException,
                   ClientRequestProcessException, ClientRequestBuildException;

    void invokeAndRedirectTo( @Nonnull WebRequest target ) throws InterruptedException,
                                                                  ClientRequestConnectException,
                                                                  ClientRequestSendException,
                                                                  ClientRequestProcessException;
}
