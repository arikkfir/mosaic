package org.mosaic.util.conversion;

import javax.annotation.Nonnull;

/**
 * @author arik
 */
public interface Converter<Source, Dest>
{
    @Nonnull
    Dest convert( @Nonnull Source source );
}
