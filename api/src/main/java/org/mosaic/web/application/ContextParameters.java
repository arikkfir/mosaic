package org.mosaic.web.application;

import java.util.List;
import javax.annotation.Nonnull;
import org.mosaic.util.collect.MapEx;

/**
 * @author arik
 */
public interface ContextParameters extends MapEx<String, String>
{
    @Nonnull
    String getName();

    @Nonnull
    List<ContextParameters> getChildren();
}
