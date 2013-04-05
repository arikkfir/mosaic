package org.mosaic.web.client;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public class ClientRequestProcessException extends ClientRequestException
{
    public ClientRequestProcessException( @Nonnull String message )
    {
        super( message );
    }

    public ClientRequestProcessException( @Nonnull String message, @Nullable Throwable cause )
    {
        super( message, cause );
    }
}
