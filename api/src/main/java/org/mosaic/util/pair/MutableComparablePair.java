package org.mosaic.util.pair;

import java.util.Comparator;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public class MutableComparablePair<L, R> extends MutablePair<L, R> implements Comparable<Pair<L, R>>
{
    @Nonnull
    public static <L, R> MutablePair<L, R> of( @Nullable L left,
                                               @Nullable R right,
                                               @Nonnull Comparator<? super Pair<L, R>> comparator )
    {
        return new MutableComparablePair<>( left, right, comparator );
    }

    @Nonnull
    private final Comparator<? super Pair<L, R>> comparator;

    public MutableComparablePair( @Nullable L left,
                                  @Nullable R right,
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
