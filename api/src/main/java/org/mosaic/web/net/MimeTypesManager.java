package org.mosaic.web.net;

import java.nio.file.Path;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public interface MimeTypesManager
{
    @Nullable
    String guessMediaType( @Nonnull String fileName );

    @Nullable
    String guessMediaType( @Nonnull Path file );
}
