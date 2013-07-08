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
    private final String name;

    @Nullable
    private final String content;

    public SnippetImpl( @Nonnull String name, @Nullable String content )
    {
        this.name = name;
        this.content = content;
    }

    @Nonnull
    @Override
    public String getName()
    {
        return this.name;
    }

    @Nonnull
    @Override
    public String getContent()
    {
        return this.content == null ? "" : this.content;
    }
}
