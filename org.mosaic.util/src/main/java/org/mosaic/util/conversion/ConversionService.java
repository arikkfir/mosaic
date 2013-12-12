package org.mosaic.util.conversion;

import com.google.common.reflect.TypeToken;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public interface ConversionService
{
    @Nullable
    <Source, Dest> Dest convert( @Nullable Source source, @Nonnull TypeToken<Dest> targetTypeToken );

    @Nullable
    <Source, Dest> Dest convert( @Nullable Source source, @Nonnull Class<Dest> targetTypeToken );
}
