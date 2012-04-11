package org.mosaic.server.shell.impl;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;
import jline.console.ConsoleReader;
import org.mosaic.server.shell.Console;

import static org.mosaic.server.shell.impl.util.StringUtils.repeat;
import static org.mosaic.server.shell.impl.util.StringUtils.rightPad;

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
    public void flush() throws IOException {
        this.consoleReader.flush();
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
    public void print( final CharSequence s ) throws IOException {
        this.consoleReader.print( s );
    }

    @Override
    public void println( final CharSequence s ) throws IOException {
        this.consoleReader.println( s );
    }

    @Override
    public void println() throws IOException {
        this.consoleReader.println();
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
    public void beep() throws IOException {
        this.consoleReader.beep();
    }

    @Override
    public boolean paste() throws IOException {
        return this.consoleReader.paste();
    }

    @Override
    public void resetPromptLine( String prompt, String buffer, int cursorDest ) throws IOException {
        this.consoleReader.resetPromptLine( prompt, buffer, cursorDest );
    }

    @Override
    public void printSearchStatus( String searchTerm, String match ) throws IOException {
        this.consoleReader.printSearchStatus( searchTerm, match );
    }

    @Override
    public void restoreLine( String originalPrompt, int cursorDest ) throws IOException {
        this.consoleReader.restoreLine( originalPrompt, cursorDest );
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

    private class Table implements TableHeaders, TablePrinter {

        private final List<String> titles = new LinkedList<>();

        private final List<Integer> lengths = new LinkedList<>();

        private int lineLength;

        private String chromeLine;

        @Override
        public TableHeaders addHeader( String title, int width ) {
            if( title.length() > width ) {
                throw new IllegalArgumentException( "Width of '" + title + "' is larger than " + width + " characters" );
            }
            this.titles.add( title );
            this.lengths.add( width );
            this.lineLength += width + 1;
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

            consoleReader.println( chromeLine );
            consoleReader.println( headerLine );
            consoleReader.println( chromeLine );
            return this;
        }

        @Override
        public TablePrinter print( Object... values ) throws IOException {
            StringBuilder line = new StringBuilder( this.lineLength );
            for( int i = 0; i < values.length; i++ ) {
                Integer colLength = this.lengths.get( i );
                Object value = values[ i ];

                String text = value == null ? "" : value.toString();
                if( text.length() > colLength ) {
                    text = text.substring( 0, colLength );
                }

                line.append( '|' ).append( rightPad( text, colLength ) );
            }
            line.append( '|' );

            consoleReader.println( line );
            return this;
        }

        @Override
        public void done() throws IOException {
            consoleReader.println( chromeLine );
        }
    }
}
