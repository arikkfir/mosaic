package org.mosaic.util.conversion;

import javax.annotation.Nonnull;

/**
 * A converter from one type to another.
 * <p/>
 * Converters are registered as Mosaic services, and all conversion path logic is provided automatically by the
 * {@link org.mosaic.util.conversion.ConversionService ConversionService} (which is also a Mosaic service available
 * for consumption). By registering possible tuples of conversions, the conversion service will discover the shortest
 * path to convert different types, even if doing so will require using multiple converters.
 *
 * @author arik
 */
public interface Converter<Source, Dest>
{
    /**
     * Perform the conversion.
     * <p/>
     * The conversion framework excludes {@code null} handling from the conversion process by always returning {@code null}
     * when {@code null} is provided; thus, converters need not worry about receiving nor returning {@code null}s.
     *
     * @param source source value to convert
     * @return converted value
     */
    @Nonnull
    Dest convert( @Nonnull Source source );
}
