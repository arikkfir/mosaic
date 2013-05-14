package org.mosaic.web.net;

import java.nio.file.Path;

/**
 * @author arik
 */
public interface MimeTypesManager
{
    String guessMediaType( String fileName );

    String guessMediaType( Path file );
}
