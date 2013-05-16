package org.mosaic.util.mail;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.Nonnull;

/**
 * @author arik
 */
public class Attachment
{
    @Nonnull
    private final String id;

    @Nonnull
    private final Path resource;

    public Attachment( @Nonnull String id, @Nonnull Path resource )
    {
        this.id = id;
        this.resource = resource;
    }

    @Nonnull
    public Path getFile()
    {
        return this.resource;
    }

    @Nonnull
    public String getId()
    {
        return this.id;
    }

    public long getContentLength() throws IOException
    {
        return Files.size( this.resource );
    }

    public long getLastModified() throws IOException
    {
        return Files.getLastModifiedTime( this.resource ).toMillis();
    }

    @Nonnull
    public String getName()
    {
        return this.resource.getFileName().toString();
    }

    @Nonnull
    public URI getUri()
    {
        return this.resource.toUri();
    }

    @Override
    @Nonnull
    public String toString()
    {
        return "Attachment[id=" + this.id + ", resource=" + this.resource + "]";
    }
}
