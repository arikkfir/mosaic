package org.mosaic.web.application;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.expression.Expression;
import org.mosaic.web.net.MediaType;

/**
 * @author arik
 */
public interface Page
{
    @Nonnull
    WebApplication getApplication();

    @Nonnull
    String getName();

    boolean isAbstract();

    boolean isActive();

    @Nullable
    Page getParent();

    @Nonnull
    String getDisplayName();

    @Nonnull
    Set<String> getTags();

    @Nullable
    Expression getSecurity();

    @Nullable
    Expression getFilter();

    @Nonnull
    MediaType getMediaType();

    long getSecondsToCache();

    @Nonnull
    Set<String> getPaths();

    @Nonnull
    Set<String> getPaths( @Nonnull String language );

    @Nonnull
    Collection<ContextProvider> getContext();

    @Nonnull
    Map<String, Block> getBlocks();
}
