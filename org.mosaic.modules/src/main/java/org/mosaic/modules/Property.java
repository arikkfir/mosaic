package org.mosaic.modules;

import javax.annotation.Nonnull;
import org.mosaic.util.pair.Pair;

/**
 * @author arik
 */
public final class Property extends Pair<String, Object>
{
    @Nonnull
    public static Property property( @Nonnull String name, @Nonnull Object value )
    {
        return new Property( name, value );
    }

    private Property( @Nonnull String left, @Nonnull Object right )
    {
        super( left, right );
    }
}
