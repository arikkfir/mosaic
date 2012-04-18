package org.mosaic.server.shell.impl.session;

import java.io.EOFException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import jline.console.ConsoleReader;
import org.mosaic.server.shell.console.Console;

import static java.lang.Math.max;
import static java.lang.Thread.currentThread;
import static org.mosaic.server.shell.impl.util.StringUtils.*;

/**
 * @author arik
 */
public class ConsoleImpl implements Console {

    private final ConsoleReader consoleReader;

    public ConsoleImpl( ConsoleReader consoleReader ) {
        this.consoleReader = consoleReader;
    }

    @Override
    public Writer getWriter() {
        return this.consoleReader.getOutput();
    }

    @Override
    public int getWidth() {
        return this.consoleReader.getTerminal().getWidth();
    }

    @Override
    public int getHeight() {
        return this.consoleReader.getTerminal().getHeight();
    }

    @Override
    public boolean setCursorPosition( int position ) throws IOException {
        return this.consoleReader.setCursorPosition( position );
    }

    @Override
    public Console flush() throws IOException {
        this.consoleReader.flush();
        return this;
    }

    @Override
    public boolean backspace() throws IOException {
        return this.consoleReader.backspace();
    }

    @Override
    public int moveCursor( int num ) throws IOException {
        return this.consoleReader.moveCursor( num );
    }

    @Override
    public boolean replace( int num, String replacement ) throws IOException {
        boolean replace = this.consoleReader.replace( num, replacement );
        flush();
        return replace;
    }

    @Override
    public int readCharacter() throws IOException {
        return this.consoleReader.readCharacter();
    }

    @Override
    public int readCharacter( final char... allowed ) throws IOException {
        Arrays.sort( allowed ); // always need to sort before binarySearch
        while( !currentThread().isInterrupted() ) {
            int i = readCharacter();
            if( i < 0 ) {
                throw new EOFException( "Connection terminated" );
            }

            char c = ( char ) i;
            if( Arrays.binarySearch( allowed, c ) >= 0 ) {
                return c;
            }
        }
        throw new EOFException( "Connection terminated" );
    }

    @Override
    public int ask( String question, char... allowed ) throws IOException {
        this.consoleReader.print( question );
        this.consoleReader.print( " [" );
        for( int i = 0, allowedLength = allowed.length; i < allowedLength; i++ ) {
            if( i > 0 ) {
                this.consoleReader.print( "/" );
            }
            this.consoleReader.print( allowed[ i ] + "" );
        }
        this.consoleReader.print( "] " );
        this.consoleReader.flush();
        return readCharacter( allowed );
    }

    @Override
    public String readLine() throws IOException {
        return this.consoleReader.readLine();
    }

    @Override
    public Console print( Object s ) throws IOException {
        this.consoleReader.print( s == null ? "" : s.toString() );
        flush();
        return this;
    }

    @Override
    public Console println() throws IOException {
        this.consoleReader.println();
        flush();
        return this;
    }

    @Override
    public Console println( Object s ) throws IOException {
        this.consoleReader.println( s == null ? "" : s.toString() );
        flush();
        return this;
    }

    @Override
    public boolean delete() throws IOException {
        return this.consoleReader.delete();
    }

    @Override
    public boolean killLine() throws IOException {
        return this.consoleReader.killLine();
    }

    @Override
    public boolean clearScreen() throws IOException {
        return this.consoleReader.clearScreen();
    }

    @Override
    public Console beep() throws IOException {
        this.consoleReader.beep();
        return this;
    }

    @Override
    public boolean paste() throws IOException {
        boolean paste = this.consoleReader.paste();
        flush();
        return paste;
    }

    @Override
    public TableHeaders createTable() {
        return new Table();
    }

    @Override
    public TableHeaders createTable( int indent ) {
        return new Table( indent );
    }

    @Override
    public Console printStackTrace( Throwable throwable ) throws IOException {
        PrintWriter printWriter = new PrintWriter( getWriter() );
        throwable.printStackTrace( printWriter );
        printWriter.flush();
        flush();
        return this;
    }

    @Override
    public Console printStackTrace( String message, Throwable throwable ) throws IOException {
        this.consoleReader.println( message );
        return printStackTrace( throwable );
    }

    private class Table implements TableHeaders, TablePrinter {

        private final String indent;

        private final List<String> titles = new LinkedList<>();

        private final List<Integer> lengths = new LinkedList<>();

        private int lineLength;

        private String chromeLine;

        private Table() {
            this( 0 );
        }

        private Table( int indent ) {
            this.indent = repeat( ' ', indent );
        }

        @Override
        public TableHeaders addHeader( String title, int width ) {
            if( title.length() > width ) {
                throw new IllegalArgumentException( "Width of '" + title + "' is larger than " + width + " characters" );
            }
            this.titles.add( title );
            this.lengths.add( width );
            this.lineLength += width + 1;
            //FEATURE 4/12/12: add support for widths by percentages (using terminal getWidth)
            return this;
        }

        @Override
        public TablePrinter start() throws IOException {
            if( this.titles.size() == 0 ) {
                throw new IllegalStateException( "No headers added to table! Call 'TableHeaders.add(..) method first" );
            }

            this.lineLength++;
            StringBuilder chromeLine = new StringBuilder( this.lineLength );
            StringBuilder headerLine = new StringBuilder( this.lineLength );
            for( int i = 0; i < this.titles.size(); i++ ) {
                int length = this.lengths.get( i );
                chromeLine.append( '+' ).append( repeat( '-', length ) );
                headerLine.append( '|' ).append( rightPad( this.titles.get( i ), length ) );
            }
            chromeLine.append( '+' );
            headerLine.append( '|' );
            this.chromeLine = chromeLine.toString();

            ConsoleImpl.this.print( this.indent ).println( chromeLine );
            ConsoleImpl.this.print( this.indent ).println( headerLine );
            ConsoleImpl.this.print( this.indent ).println( chromeLine );
            return this;
        }

        @Override
        public TablePrinter print( Object... values ) throws IOException {

            // build a matrix of values - outer list is the columns, and each inner list is lines for that column value
            List<List<String>> matrix = new ArrayList<>( values.length );
            int lineCount = 1;
            for( int colIndex = 0; colIndex < this.titles.size(); colIndex++ ) {
                Integer colLength = this.lengths.get( colIndex );
                Object value;
                if( colIndex >= values.length || values[ colIndex ] == null ) {
                    value = "";
                } else {
                    value = values[ colIndex ];
                }

                List<String> valueLines = splitLinesOnLengthAndWords( value.toString(), colLength );
                matrix.add( valueLines );
                lineCount = max( valueLines.size(), lineCount );
            }

            StringBuilder buffer = new StringBuilder( this.lineLength );
            for( int lineIndex = 0; lineIndex < lineCount; lineIndex++ ) {
                buffer.delete( 0, Integer.MAX_VALUE );
                for( int colIndex = 0; colIndex < this.titles.size(); colIndex++ ) {
                    Integer colLength = this.lengths.get( colIndex );
                    List<String> colLines = matrix.get( colIndex );
                    String line = lineIndex < colLines.size()
                            ? rightPad( colLines.get( lineIndex ), colLength )
                            : repeat( ' ', colLength );
                    buffer.append( '|' ).append( line );
                }
                buffer.append( '|' );
                ConsoleImpl.this.print( this.indent ).println( buffer );
            }

            return this;
        }

        @Override
        public void done() throws IOException {
            ConsoleImpl.this.print( this.indent ).println( chromeLine );
        }
    }
}
