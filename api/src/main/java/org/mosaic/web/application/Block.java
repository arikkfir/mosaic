package org.mosaic.web.application;

import java.util.Collection;
import javax.annotation.Nonnull;

/**
 * @author arik
 */
public interface Block
{
    @Nonnull
    Panel getPanel();

    @Nonnull
    String getName();

    @Nonnull
    String getDisplayName();

    @Nonnull
    Snippet getSnippet();

    @Nonnull
    Collection<ContextProviderRef> getContext();
}
