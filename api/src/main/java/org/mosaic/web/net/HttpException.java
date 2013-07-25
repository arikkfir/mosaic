package org.mosaic.web.net;

import javax.annotation.Nonnull;

/**
 * @author arik
 */
public class HttpException extends RuntimeException
{
    @Nonnull
    private final HttpStatus status;

    public HttpException( String message, @Nonnull HttpStatus status )
    {
        super( message );
        this.status = status;
    }

    @Nonnull
    public HttpStatus getStatus()
    {
        return this.status;
    }
}
