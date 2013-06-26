package org.mosaic.web.request.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.Nonnull;
import javax.servlet.http.Part;
import org.mosaic.web.net.MediaType;
import org.mosaic.web.request.WebPart;

/**
 * @author arik
 */
public class WebPartImpl implements WebPart
{
    @Nonnull
    private final Part part;

    public WebPartImpl( @Nonnull Part part )
    {
        this.part = part;
    }

    @Nonnull
    @Override
    public String getName()
    {
        return this.part.getName();
    }

    @Nonnull
    @Override
    public MediaType getContentType()
    {
        return new MediaType( this.part.getContentType() );
    }

    @Override
    public long getSize()
    {
        return this.part.getSize();
    }

    @Nonnull
    @Override
    public InputStream getBinaryContents() throws IOException
    {
        return this.part.getInputStream();
    }

    @Nonnull
    @Override
    public Reader getCharacterContents() throws IOException
    {
        MediaType contentType = getContentType();
        Charset charset = contentType.getCharset();
        if( charset == null )
        {
            charset = Charset.forName( "UTF-8" );
        }
        return new InputStreamReader( getBinaryContents(), charset );
    }

    @Override
    public void saveLocally( @Nonnull Path file ) throws IOException
    {
        this.part.write( file.toString() );
    }

    @Nonnull
    @Override
    public Path saveToTempFile() throws IOException
    {
        Path file = Files.createTempFile( "mosaic-web-", "tmp" );
        saveLocally( file );
        file.toFile().deleteOnExit();
        return file;
    }
}
