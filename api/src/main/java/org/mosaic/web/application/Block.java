package org.mosaic.web.application;

import java.util.Collection;
import javax.annotation.Nonnull;

/**
 * @author arik
 */
public interface Block
{
    @Nonnull
    Page getPage();

    @Nonnull
    String getName();

    @Nonnull
    String getDisplayName();

    @Nonnull
    Collection<ContextProvider> getContext();

    @Nonnull
    Snippet getSnippet();
}
