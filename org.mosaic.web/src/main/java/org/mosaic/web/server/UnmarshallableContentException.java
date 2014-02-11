package org.mosaic.web.server;

import com.google.common.net.MediaType;
import java.util.List;
import javax.annotation.Nonnull;

/**
 * @author arik
 */
public class UnmarshallableContentException extends RuntimeException
{
    @Nonnull
    private final Object value;

    @Nonnull
    private final List<MediaType> allowedMediaTypes;

    public UnmarshallableContentException( @Nonnull Object value, @Nonnull List<MediaType> allowedMediaTypes )
    {
        super( "could not marshall '" + value + "' into any of '" + allowedMediaTypes + "'" );
        this.value = value;
        this.allowedMediaTypes = allowedMediaTypes;
    }

    @Nonnull
    public Object getValue()
    {
        return value;
    }

    @Nonnull
    public List<MediaType> getAllowedMediaTypes()
    {
        return allowedMediaTypes;
    }
}
