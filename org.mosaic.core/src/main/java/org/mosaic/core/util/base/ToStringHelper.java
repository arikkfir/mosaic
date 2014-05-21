package org.mosaic.core.util.base;

import org.mosaic.core.util.Nullable;

import static java.util.Objects.requireNonNull;

public final class ToStringHelper
{
    public static ToStringHelper create( Object self )
    {
        return new ToStringHelper( self.getClass().getSimpleName() );
    }

    public static ToStringHelper create( Class<?> clazz )
    {
        return new ToStringHelper( clazz.getSimpleName() );
    }

    public static ToStringHelper create( String className )
    {
        return new ToStringHelper( className );
    }

    private static final class ValueHolder
    {
        String name;

        Object value;

        ValueHolder next;
    }

    private final String className;

    private ValueHolder holderHead = new ValueHolder();

    private ValueHolder holderTail = holderHead;

    private boolean omitNullValues = false;

    private ToStringHelper( String className )
    {
        this.className = requireNonNull( className );
    }

    public ToStringHelper omitNullValues()
    {
        omitNullValues = true;
        return this;
    }

    public ToStringHelper add( String name, @Nullable Object value )
    {
        return addHolder( name, value );
    }

    public ToStringHelper add( String name, boolean value )
    {
        return addHolder( name, String.valueOf( value ) );
    }

    public ToStringHelper add( String name, char value )
    {
        return addHolder( name, String.valueOf( value ) );
    }

    public ToStringHelper add( String name, double value )
    {
        return addHolder( name, String.valueOf( value ) );
    }

    public ToStringHelper add( String name, float value )
    {
        return addHolder( name, String.valueOf( value ) );
    }

    public ToStringHelper add( String name, int value )
    {
        return addHolder( name, String.valueOf( value ) );
    }

    public ToStringHelper add( String name, long value )
    {
        return addHolder( name, String.valueOf( value ) );
    }

    public ToStringHelper addValue( @Nullable Object value )
    {
        return addHolder( value );
    }

    public ToStringHelper addValue( boolean value )
    {
        return addHolder( String.valueOf( value ) );
    }

    public ToStringHelper addValue( char value )
    {
        return addHolder( String.valueOf( value ) );
    }

    public ToStringHelper addValue( double value )
    {
        return addHolder( String.valueOf( value ) );
    }

    public ToStringHelper addValue( float value )
    {
        return addHolder( String.valueOf( value ) );
    }

    public ToStringHelper addValue( int value )
    {
        return addHolder( String.valueOf( value ) );
    }

    public ToStringHelper addValue( long value )
    {
        return addHolder( String.valueOf( value ) );
    }

    @Override
    public String toString()
    {
        boolean omitNullValuesSnapshot = omitNullValues;
        StringBuilder builder = new StringBuilder( 32 );
        for( ValueHolder valueHolder = holderHead.next; valueHolder != null; valueHolder = valueHolder.next )
        {
            if( !omitNullValuesSnapshot || valueHolder.value != null )
            {
                if( builder.length() > 0 )
                {
                    builder.append( ", " );
                }

                if( valueHolder.name != null )
                {
                    builder.append( valueHolder.name ).append( '=' );
                }
                builder.append( valueHolder.value );
            }
        }
        return builder.length() == 0 ? className : className + "[ " + builder + " ]";
    }

    private ValueHolder addHolder()
    {
        ValueHolder valueHolder = new ValueHolder();
        holderTail = holderTail.next = valueHolder;
        return valueHolder;
    }

    private ToStringHelper addHolder( @Nullable Object value )
    {
        ValueHolder valueHolder = addHolder();
        valueHolder.value = value;
        return this;
    }

    private ToStringHelper addHolder( String name, @Nullable Object value )
    {
        ValueHolder valueHolder = addHolder();
        valueHolder.value = value;
        valueHolder.name = requireNonNull( name );
        return this;
    }
}
