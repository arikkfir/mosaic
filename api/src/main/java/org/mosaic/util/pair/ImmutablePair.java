package org.mosaic.util.pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public class ImmutablePair<L, R> extends Pair<L, R>
{
    @Nonnull
    public static <L, R> ImmutablePair<L, R> of( @Nullable L left, @Nullable R right )
    {
        return new ImmutablePair<>( left, right );
    }

    protected ImmutablePair( @Nullable L left, @Nullable R right )
    {
        super( left, right );
    }
}
