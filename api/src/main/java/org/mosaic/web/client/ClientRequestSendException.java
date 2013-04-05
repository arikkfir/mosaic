package org.mosaic.web.client;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public class ClientRequestSendException extends ClientRequestException
{
    public ClientRequestSendException( @Nonnull String message )
    {
        super( message );
    }

    public ClientRequestSendException( @Nonnull String message, @Nullable Throwable cause )
    {
        super( message, cause );
    }
}
