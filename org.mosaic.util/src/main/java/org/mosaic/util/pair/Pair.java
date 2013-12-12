package org.mosaic.util.pair;

import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
@SuppressWarnings( "WeakerAccess" )
public class Pair<L, R> implements Map.Entry<L, R>
{
    @Nonnull
    public static <L, R> Pair<L, R> of( L left, R right )
    {
        return new Pair<>( left, right );
    }

    private L left;

    private R right;

    protected Pair( L left, R right )
    {
        this.left = left;
        this.right = right;
    }

    public L getLeft()
    {
        return this.left;
    }

    protected void setLeft( L left )
    {
        this.left = left;
    }

    public R getRight()
    {
        return this.right;
    }

    protected void setRight( R right )
    {
        this.right = right;
    }

    @Override
    public final L getKey()
    {
        return getLeft();
    }

    @Override
    public final R getValue()
    {
        return getRight();
    }

    @Override
    public R setValue( R value )
    {
        R old = getRight();
        setRight( value );
        return old;
    }

    @Override
    public final boolean equals( @Nullable Object obj )
    {
        if( obj == this )
        {
            return true;
        }
        else if( obj instanceof Pair )
        {
            Pair other = ( Pair ) obj;
            return Objects.equals( getLeft(), other.getLeft() ) && Objects.equals( getRight(), other.getRight() );
        }
        else if( obj instanceof Map.Entry<?, ?> )
        {
            Map.Entry<?, ?> other = ( Map.Entry<?, ?> ) obj;
            return Objects.equals( getLeft(), other.getKey() ) && Objects.equals( getRight(), other.getValue() );
        }
        return false;
    }

    @Override
    public final int hashCode()
    {
        return ( getLeft() == null ? 0 : getLeft().hashCode() ) ^ ( getRight() == null ? 0 : getRight().hashCode() );
    }

    @Nonnull
    @Override
    public final String toString()
    {
        return getClass().getSimpleName() + "<" + getLeft() + ", " + getRight() + ">";
    }
}
