package org.mosaic.web.client;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public class ClientRequestConnectException extends ClientRequestException
{
    public ClientRequestConnectException( @Nonnull String message )
    {
        super( message );
    }

    public ClientRequestConnectException( @Nonnull String message, @Nullable Throwable cause )
    {
        super( message, cause );
    }
}
