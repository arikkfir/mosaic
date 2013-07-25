package org.mosaic.web.application;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.expression.Expression;

/**
 * @author arik
 */
public interface Page
{
    @Nonnull
    WebContent getWebContent();

    @Nonnull
    String getName();

    @Nonnull
    String getDisplayName();

    boolean isActive();

    @Nonnull
    Template getTemplate();

    @Nullable
    Expression getSecurity();

    @Nonnull
    Set<String> getPaths();

    @Nonnull
    Set<String> getPaths( @Nonnull String language );

    @Nonnull
    Collection<ContextProviderRef> getContext();

    @Nonnull
    List<Block> getBlocks( @Nonnull String panelName );
}
