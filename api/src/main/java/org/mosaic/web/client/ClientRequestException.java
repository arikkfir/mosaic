package org.mosaic.web.client;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public class ClientRequestException extends RuntimeException
{
    public ClientRequestException( @Nonnull String message )
    {
        super( message );
    }

    public ClientRequestException( @Nonnull String message, @Nullable Throwable cause )
    {
        super( message, cause );
    }
}
