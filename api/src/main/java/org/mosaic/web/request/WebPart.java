package org.mosaic.web.request;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.file.Path;
import javax.annotation.Nonnull;
import org.mosaic.net.MediaType;

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
    ByteBuffer getBinaryContents();

    @Nonnull
    CharBuffer getCharacterContents();

    void saveLocally( @Nonnull Path file ) throws IOException;
}
