package org.mosaic.server.shell.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import jline.console.ConsoleReader;
import org.mosaic.server.shell.Console;

import static java.lang.Math.max;
import static org.mosaic.server.shell.impl.util.StringUtils.*;

/**
 * @author arik
 */
public class ShellConsole implements Console {

    private final ConsoleReader consoleReader;

    public ShellConsole( ConsoleReader consoleReader ) {
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
    public boolean replace( int num, String replacement ) {
        return this.consoleReader.replace( num, replacement );
    }

    @Override
    public int readCharacter() throws IOException {
        return this.consoleReader.readCharacter();
    }

    @Override
    public int readCharacter( final char... allowed ) throws IOException {
        return this.consoleReader.readCharacter( allowed );
    }

    @Override
    public String readLine() throws IOException {
        return this.consoleReader.readLine();
    }

    @Override
    public String readLine( Character mask ) throws IOException {
        return this.consoleReader.readLine( mask );
    }

    @Override
    public String readLine( String prompt ) throws IOException {
        return this.consoleReader.readLine( prompt );
    }

    @Override
    public String readLine( String prompt, Character mask ) throws IOException {
        return this.consoleReader.readLine( prompt, mask );
    }

    @Override
    public Console print( final CharSequence s ) throws IOException {
        this.consoleReader.print( s );
        flush();
        return this;
    }

    @Override
    public Console println( final CharSequence s ) throws IOException {
        this.consoleReader.println( s );
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
    public Console resetPromptLine( String prompt, String buffer, int cursorDest ) throws IOException {
        this.consoleReader.resetPromptLine( prompt, buffer, cursorDest );
        flush();
        return this;
    }

    @Override
    public Console printSearchStatus( String searchTerm, String match ) throws IOException {
        this.consoleReader.printSearchStatus( searchTerm, match );
        flush();
        return this;
    }

    @Override
    public Console restoreLine( String originalPrompt, int cursorDest ) throws IOException {
        this.consoleReader.restoreLine( originalPrompt, cursorDest );
        flush();
        return this;
    }

    @Override
    public int searchBackwards( String searchTerm, int startIndex ) {
        return this.consoleReader.searchBackwards( searchTerm, startIndex );
    }

    @Override
    public int searchBackwards( String searchTerm ) {
        return this.consoleReader.searchBackwards( searchTerm );
    }

    @Override
    public int searchBackwards( String searchTerm, int startIndex, boolean startsWith ) {
        return this.consoleReader.searchBackwards( searchTerm, startIndex, startsWith );
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
        println( message );
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
            //TODO 4/12/12: add support for widths by percentages (using terminal getWidth)
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

            ShellConsole.this.print( this.indent ).println( chromeLine );
            ShellConsole.this.print( this.indent ).println( headerLine );
            ShellConsole.this.print( this.indent ).println( chromeLine );
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
                ShellConsole.this.print( this.indent ).println( buffer );
            }

            return this;
        }

        @Override
        public void done() throws IOException {
            ShellConsole.this.print( this.indent ).println( chromeLine );
        }
    }
}
