package org.mosaic.util.pair;

import com.google.common.collect.ComparisonChain;
import java.util.Comparator;

/**
 * @author arik
 */
public class PairComparator<L extends Comparable<L>, R extends Comparable<R>> implements Comparator<Pair<L, R>>
{
    @Override
    public int compare( Pair<L, R> o1, Pair<L, R> o2 )
    {
        if( o1 == o2 )
        {
            return 0;
        }
        else if( o1 == null )
        {
            return -1;
        }
        else if( o2 == null )
        {
            return 1;
        }
        else
        {
            return ComparisonChain.start()
                                  .compare( o1.getLeft(), o2.getLeft() )
                                  .compare( o1.getRight(), o2.getRight() )
                                  .result();
        }
    }
}
