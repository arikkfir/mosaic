package org.mosaic.console.remote.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jline.console.ConsoleReader;
import jline.console.Operation;
import org.mosaic.console.Console;
import org.mosaic.console.spi.CommandCanceledException;
import org.mosaic.console.spi.QuitSessionException;
import org.slf4j.helpers.FormattingTuple;

import static java.util.Arrays.binarySearch;
import static java.util.Arrays.sort;
import static org.slf4j.helpers.MessageFormatter.arrayFormat;

/**
 * @author arik
 */
final class ConsoleImpl implements Console
{
    @Nonnull
    private final ConsoleReader consoleReader;

    ConsoleImpl( @Nonnull ConsoleReader consoleReader )
    {
        this.consoleReader = consoleReader;
    }

    @Override
    public final int getWidth()
    {
        return this.consoleReader.getTerminal().getWidth();
    }

    @Override
    public final int getHeight()
    {
        return this.consoleReader.getTerminal().getHeight();
    }

    @Override
    public final char restrictedRead( char... allowed ) throws IOException
    {
        sort( allowed );
        while( true )
        {
            int c = this.consoleReader.readCharacter();
            if( c == -1 )
            {
                return ( char ) c;
            }

            if( binarySearch( allowed, ( char ) c ) >= 0 )
            {
                return ( char ) c;
            }

            StringBuilder sb = new StringBuilder();
            sb.appendCodePoint( c );

            Object o = this.consoleReader.getKeys().getBound( sb );
            if( o instanceof Operation )
            {
                switch( ( Operation ) o )
                {
                    case EXIT_OR_DELETE_CHAR:
                        println();
                        this.consoleReader.flush();
                        throw new QuitSessionException();

                    case INTERRUPT:
                        println();
                        this.consoleReader.flush();
                        throw new CommandCanceledException();

                    default:
                        continue;
                }
            }

            this.consoleReader.beep();
        }
    }

    @Override
    public final char restrictedPrompt( @Nonnull String prompt, char... allowed ) throws IOException
    {
        print( prompt );

        print( '[' );
        boolean first = true;
        for( char c : allowed )
        {
            if( !first )
            {
                print( '/' );
            }
            first = false;
            print( c );
        }
        print( "] " );
        return print( prompt ).restrictedRead( allowed );
    }

    @Nullable
    @Override
    public final String readLine() throws IOException
    {
        try
        {
            return this.consoleReader.readLine();
        }
        catch( jline.console.UserInterruptException e )
        {
            throw new CommandCanceledException();
        }
    }

    @Nonnull
    @Override
    public final Console print( @Nullable Object value, @Nullable Object... args ) throws IOException
    {
        this.consoleReader.print( arrayFormat( Objects.toString( value, "" ), args ).getMessage() );
        this.consoleReader.flush();
        return this;
    }

    @Nonnull
    @Override
    public final Console println() throws IOException
    {
        this.consoleReader.println();
        this.consoleReader.flush();
        return this;
    }

    @Nonnull
    @Override
    public final Console println( @Nullable Object value, @Nullable Object... args ) throws IOException
    {
        FormattingTuple tuple = arrayFormat( Objects.toString( value, "" ), args );
        this.consoleReader.println( tuple.getMessage() );

        @SuppressWarnings( "ThrowableResultOfMethodCallIgnored" )
        Throwable throwable = tuple.getThrowable();
        if( throwable != null )
        {
            printStackTrace( throwable );
        }

        this.consoleReader.flush();
        return this;
    }

    @Nonnull
    @Override
    public final Console printStackTrace( @Nonnull Throwable throwable ) throws IOException
    {
        StringWriter stackTrace = new StringWriter( 1000 );
        throwable.printStackTrace( new PrintWriter( stackTrace, true ) );
        this.consoleReader.println( stackTrace.toString() );
        this.consoleReader.flush();
        return this;
    }
}
