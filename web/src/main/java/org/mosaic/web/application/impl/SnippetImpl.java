package org.mosaic.web.application.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.web.application.Snippet;

/**
 * @author arik
 */
public class SnippetImpl implements Snippet
{
    @Nonnull
    private final String id;

    @Nullable
    private final String content;

    public SnippetImpl( @Nonnull String id, @Nullable String content )
    {
        this.id = id;
        this.content = content;
    }

    @Nonnull
    @Override
    public String getId()
    {
        return this.id;
    }

    @Nonnull
    @Override
    public String getContent()
    {
        return this.content == null ? "" : this.content;
    }
}
