package org.mosaic.web.server;

import com.google.common.net.MediaType;
import java.io.IOException;
import java.io.OutputStream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public interface MessageMarshaller
{
    boolean canMarshall( @Nonnull Object value, @Nonnull MediaType mediaType );

    void marshall( @Nonnull MarshallingSink sink ) throws Exception;

    interface MarshallingSink
    {
        @Nullable
        MediaType getContentType();

        void setContentType( @Nullable MediaType mediaType );

        @Nonnull
        Object getValue();

        @Nonnull
        OutputStream getOutputStream() throws IOException;
    }
}
