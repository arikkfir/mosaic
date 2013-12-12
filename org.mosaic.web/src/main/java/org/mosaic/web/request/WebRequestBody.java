package org.mosaic.web.request;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public interface WebRequestBody
{
    @Nonnull
    InputStream stream() throws IOException;

    @Nonnull
    Reader reader() throws IOException;

    @Nullable
    WebRequestPart getPart( @Nonnull String name ) throws IOException;

    @Nonnull
    Map<String, WebRequestPart> getPartsMap() throws IOException;
}
