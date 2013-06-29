package org.mosaic.util.pair;

import javax.annotation.Nonnull;

/**
 * @author arik
 */
public class MutablePair<L, R> extends Pair<L, R>
{
    @Nonnull
    public static <L, R> MutablePair<L, R> of( L left, R right )
    {
        return new MutablePair<>( left, right );
    }

    public MutablePair( L left, R right )
    {
        super( left, right );
    }

    @Override
    public R setValue( R value )
    {
        R old = getRight();
        setRight( value );
        return old;
    }
}
