package org.mosaic.web.request;

import com.google.common.net.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Path;
import javax.annotation.Nonnull;

/**
 * @author arik
 */
public interface WebRequestPart
{
    @Nonnull
    String getName();

    @Nonnull
    MediaType getContentType();

    long getSize();

    @Nonnull
    InputStream stream() throws IOException;

    @Nonnull
    Reader reader() throws IOException;

    void saveLocally( @Nonnull Path file ) throws IOException;

    @Nonnull
    Path saveToTempFile() throws IOException;
}
