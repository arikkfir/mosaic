package org.mosaic.util.pair;

import javax.annotation.Nonnull;

/**
 * @author arik
 */
public class ImmutablePair<L, R> extends Pair<L, R>
{
    @Nonnull
    public static <L, R> ImmutablePair<L, R> of( L left, R right )
    {
        return new ImmutablePair<>( left, right );
    }

    protected ImmutablePair( L left, R right )
    {
        super( left, right );
    }
}
