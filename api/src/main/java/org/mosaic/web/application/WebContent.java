package org.mosaic.web.application;

import java.nio.file.Path;
import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joda.time.Period;

/**
 * @author arik
 */
public interface WebContent
{
    @Nonnull
    WebApplication getApplication();

    @Nonnull
    Collection<Path> getContentRoots();

    @Nonnull
    Collection<ContextProviderRef> getContext();

    @Nullable
    Snippet getSnippet( @Nonnull String name );

    @Nonnull
    Collection<Snippet> getSnippets();

    @Nullable
    Template getTemplate( @Nonnull String name );

    @Nonnull
    Collection<Template> getTemplates();

    @Nullable
    Page getPage( @Nonnull String name );

    @Nonnull
    Collection<Page> getPages();

    @Nullable
    Period getCachePeriod( @Nonnull String path );
}
