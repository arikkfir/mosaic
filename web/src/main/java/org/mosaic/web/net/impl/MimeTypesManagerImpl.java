package org.mosaic.web.net.impl;

import java.nio.file.Path;
import javax.activation.MimetypesFileTypeMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.lifecycle.annotation.Service;
import org.mosaic.web.net.MimeTypesManager;

/**
 * @author arik
 */
@Service(MimeTypesManager.class)
public class MimeTypesManagerImpl implements MimeTypesManager
{
    @Nonnull
    private final MimetypesFileTypeMap mimetypesFileTypeMap = new MimetypesFileTypeMap();

    @Nullable
    @Override
    public String guessMediaType( @Nonnull String fileName )
    {
        return this.mimetypesFileTypeMap.getContentType( fileName );
    }

    @Nullable
    @Override
    public String guessMediaType( @Nonnull Path file )
    {
        return this.mimetypesFileTypeMap.getContentType( file.toFile() );
    }
}
