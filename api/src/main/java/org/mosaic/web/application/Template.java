package org.mosaic.web.application;

import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public interface Template
{
    @Nonnull
    WebContent getWebContent();

    @Nonnull
    String getName();

    @Nonnull
    String getDisplayName();

    @Nonnull
    Snippet getSnippet();

    @Nonnull
    Collection<ContextProviderRef> getContext();

    @Nullable
    Panel getPanel( @Nonnull String name );

    @Nonnull
    Collection<Panel> getPanels();
}
