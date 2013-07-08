package org.mosaic.web.application;

import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;

/**
 * @author arik
 */
public interface Panel
{
    @Nonnull
    Template getTemplate();

    @Nonnull
    String getName();

    @Nonnull
    String getDisplayName();

    @Nonnull
    Collection<ContextProviderRef> getContext();

    @Nonnull
    List<Block> getBlocks();
}
