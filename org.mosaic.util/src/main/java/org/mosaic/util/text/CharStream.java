package org.mosaic.util.text;

import java.util.Arrays;
import javax.annotation.Nonnull;
import org.mosaic.util.pair.Pair;

import static java.lang.Math.abs;

/**
 * @author arik
 */
public class CharStream implements CharSequence
{
    @Nonnull
    private final char[] chars;

    private int next = 0;

    public CharStream( @Nonnull String chars )
    {
        this( chars.toCharArray() );
    }

    public CharStream( @Nonnull char[] chars )
    {
        this.chars = chars;
    }

    public CharStream( @Nonnull char[] chars, int from )
    {
        this( chars, from, chars.length );
    }

    public CharStream( @Nonnull char[] chars, int from, int to )
    {
        this.chars = Arrays.copyOfRange( chars, from, to );
    }

    public CharStream( @Nonnull CharSequence chars )
    {
        this.chars = new char[ chars.length() ];
        for( int i = 0; i < chars.length(); i++ )
        {
            this.chars[ i ] = chars.charAt( i );
        }
    }

    @Override
    public final int length()
    {
        return this.chars.length;
    }

    @Override
    public final char charAt( int index )
    {
        return this.chars[ index ];
    }

    @Override
    public final CharSequence subSequence( int start, int end )
    {
        return new CharStream( this.chars, start, end );
    }

    public final boolean hasNext()
    {
        return this.next < this.chars.length;
    }

    public final char next()
    {
        if( hasNext() )
        {
            return this.chars[ this.next++ ];
        }
        else
        {
            throw new IllegalStateException( "CharStream exausted" );
        }
    }

    public final char peek()
    {
        if( hasNext() )
        {
            return this.chars[ this.next ];
        }
        else
        {
            throw new IllegalStateException( "CharStream exausted" );
        }
    }

    @Nonnull
    public final CharStream skipWhitespace()
    {
        while( hasNext() && Character.isWhitespace( peek() ) )
        {
            this.next++;
        }
        return this;
    }

    @Nonnull
    public final String readWhileNotAnyOf( @Nonnull String stopChars )
    {
        StringBuilder buffer = new StringBuilder( abs( this.chars.length - this.next ) );
        while( hasNext() && !stopChars.contains( peek() + "" ) )
        {
            buffer.append( next() );
        }
        return buffer.toString();
    }

    @Nonnull
    public final String readUntil( char stopChar )
    {
        StringBuilder buffer = new StringBuilder( abs( this.chars.length - this.next ) );
        while( hasNext() )
        {
            char c = next();
            if( c != stopChar )
            {
                buffer.append( c );
            }
            else
            {
                break;
            }
        }
        return buffer.toString();
    }

    @Nonnull
    public final Pair<String, Character> readUntilWithDelimiter( char stopChar )
    {
        char c = 0;

        StringBuilder buffer = new StringBuilder( abs( this.chars.length - this.next ) );
        while( hasNext() )
        {
            c = next();
            if( c != stopChar )
            {
                buffer.append( c );
            }
            else
            {
                break;
            }
        }
        return Pair.of( buffer.toString(), c );
    }

    @Nonnull
    public final String readUntilAnyOf( @Nonnull String stopChars )
    {
        StringBuilder buffer = new StringBuilder( abs( this.chars.length - this.next ) );
        while( hasNext() )
        {
            char c = next();
            if( !stopChars.contains( c + "" ) )
            {
                buffer.append( c );
            }
            else
            {
                break;
            }
        }
        return buffer.toString();
    }

    @Nonnull
    public final Pair<String, Character> readUntilAnyOfWithDelimiter( @Nonnull String stopChars )
    {
        char c = 0;

        StringBuilder buffer = new StringBuilder( abs( this.chars.length - this.next ) );
        while( hasNext() )
        {
            c = next();
            if( !stopChars.contains( c + "" ) )
            {
                buffer.append( c );
            }
            else
            {
                break;
            }
        }
        return Pair.of( buffer.toString(), c );
    }
}
