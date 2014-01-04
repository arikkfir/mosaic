package org.mosaic.web.application;

import java.nio.file.Path;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joda.time.Period;

/**
 * @author arik
 */
public interface Resource
{
    @Nonnull
    Path getPath();

    boolean isCompressionEnabled();

    boolean isBrowsingEnabled();

    @Nullable
    Period getCachePeriod();
}
