package org.mosaic.web.client;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public class ClientRequestBuildException extends ClientRequestException
{
    public ClientRequestBuildException( @Nonnull String message )
    {
        super( message );
    }

    public ClientRequestBuildException( @Nonnull String message, @Nullable Throwable cause )
    {
        super( message, cause );
    }
}
