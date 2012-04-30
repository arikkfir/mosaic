package org.mosaic.web;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import org.mosaic.util.collection.TypedDict;
import org.springframework.http.MediaType;

/**
 * @author arik
 */
public interface HttpPart {

    String getName();

    MediaType getContentType();

    long getSize();

    InputStream getInputStream() throws IOException;

    void save( Path path ) throws IOException;

    TypedDict<String> getHeaders();

}
