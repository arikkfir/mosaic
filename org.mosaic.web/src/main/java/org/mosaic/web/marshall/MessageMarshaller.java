package org.mosaic.web.marshall;

import com.google.common.net.MediaType;
import java.io.OutputStream;
import javax.annotation.Nonnull;

/**
 * @author arik
 */
public interface MessageMarshaller
{
    boolean canMarshall( @Nonnull Object value, @Nonnull MediaType mediaType );

    void marshall( @Nonnull Object value, @Nonnull MediaType mediaType, @Nonnull OutputStream outputStream )
            throws Exception;
}
