package org.mosaic.web.request;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Path;
import javax.annotation.Nonnull;
import org.mosaic.web.net.MediaType;

/**
 * @author arik
 */
public interface WebPart
{
    @Nonnull
    String getName();

    @Nonnull
    MediaType getContentType();

    long getSize();

    @Nonnull
    InputStream getBinaryContents() throws IOException;

    @Nonnull
    Reader getCharacterContents() throws IOException;

    void saveLocally( @Nonnull Path file ) throws IOException;

    @Nonnull
    Path saveToTempFile() throws IOException;
}
