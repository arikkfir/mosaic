package org.mosaic.util.pair;

import java.util.Comparator;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public class ImmutableComparablePair<L, R> extends ImmutablePair<L, R> implements Comparable<Pair<L, R>>
{
    @Nonnull
    public static <L, R> ImmutablePair<L, R> of( L left,
                                                 R right,
                                                 @Nonnull Comparator<? super Pair<L, R>> comparator )
    {
        return new ImmutableComparablePair<>( left, right, comparator );
    }

    @Nonnull
    private final Comparator<? super Pair<L, R>> comparator;

    protected ImmutableComparablePair( L left,
                                       R right,
                                       @Nonnull Comparator<? super Pair<L, R>> comparator )
    {
        super( left, right );
        this.comparator = comparator;
    }

    @Override
    public int compareTo( @Nullable Pair<L, R> o )
    {
        return this.comparator.compare( this, o );
    }
}
