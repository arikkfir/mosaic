package org.mosaic.util.pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public class MutablePair<L, R> extends Pair<L, R>
{
    @Nonnull
    public static <L, R> MutablePair<L, R> of( @Nullable L left, @Nullable R right )
    {
        return new MutablePair<>( left, right );
    }

    public MutablePair( @Nullable L left, @Nullable R right )
    {
        super( left, right );
    }

    @Override
    @Nullable
    public R setValue( R value )
    {
        R old = getRight();
        setRight( value );
        return old;
    }
}
