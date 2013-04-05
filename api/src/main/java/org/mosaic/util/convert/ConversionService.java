package org.mosaic.util.convert;

import com.google.common.reflect.TypeToken;
import javax.annotation.Nonnull;

/**
 * @author arik
 */
public interface ConversionService
{
    @Nonnull
    <Source, Dest> Dest convert( @Nonnull Source source, @Nonnull TypeToken<Dest> targetTypeToken );

    @Nonnull
    <Source, Dest> Dest convert( @Nonnull Source source, @Nonnull Class<Dest> targetTypeToken );
}
