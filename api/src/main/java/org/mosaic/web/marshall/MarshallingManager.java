package org.mosaic.web.marshall;

import com.google.common.reflect.TypeToken;
import java.io.InputStream;
import java.io.OutputStream;
import javax.annotation.Nonnull;
import org.mosaic.web.net.MediaType;

/**
 * @author arik
 */
public interface MarshallingManager
{
    void marshall( @Nonnull Object value,
                   @Nonnull OutputStream targetOutputStream,
                   @Nonnull MediaType... targetMediaTypes )
            throws Exception;

    <T> T unmarshall( @Nonnull InputStream sourceInputStream,
                      @Nonnull MediaType sourceMediaType,
                      @Nonnull TypeToken<? extends T> targetType ) throws Exception;
}
