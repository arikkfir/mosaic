package org.mosaic.util.collections;

import com.google.common.base.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.conversion.ConversionService;
import org.mosaic.util.osgi.SimpleServiceTracker;

/**
 * @author arik
 */
final class ConversionServiceTracker
{
    @Nonnull
    private static final SimpleServiceTracker<ConversionService> tracker =
            new SimpleServiceTracker<>( ConversionServiceTracker.class, ConversionService.class );

    @Nullable
    public static ConversionService get()
    {
        return ConversionServiceTracker.tracker.get();
    }

    @Nonnull
    public static Optional<ConversionService> optional()
    {
        return Optional.fromNullable( ConversionServiceTracker.tracker.get() );
    }

    @Nonnull
    public static ConversionService conversionService()
    {
        return ConversionServiceTracker.tracker.require();
    }

    private ConversionServiceTracker()
    {
    }
}
