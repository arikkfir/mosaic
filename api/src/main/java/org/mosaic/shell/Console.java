package org.mosaic.shell;

import java.io.IOException;
import java.io.Writer;

/**
 * @author arik
 */
public interface Console
{
    int getWidth();

    int getHeight();

    Writer getWriter();

    Console flush() throws IOException;

    Console printStackTrace( Throwable throwable ) throws IOException;

    Console printStackTrace( String message, Throwable throwable ) throws IOException;

    boolean setCursorPosition( int position ) throws IOException;

    boolean backspace() throws IOException;

    int moveCursor( int num ) throws IOException;

    boolean replace( int num, String replacement ) throws IOException;

    int readCharacter() throws IOException;

    int readCharacter( char... allowed ) throws IOException;

    int ask( String question, char... allowed ) throws IOException;

    String readLine() throws IOException;

    Console print( Object n ) throws IOException;

    Console print( int indent, Object s ) throws IOException;

    Console println() throws IOException;

    Console println( Object n ) throws IOException;

    Console println( int indent, Object s ) throws IOException;

    Console printlnNoFirstLineIndent( int indent, Object s ) throws IOException;

    boolean delete() throws IOException;

    boolean killLine() throws IOException;

    boolean clearScreen() throws IOException;

    Console beep() throws IOException;

    boolean paste() throws IOException;

    TableHeaders createTable();

    TableHeaders createTable( int indent );

    interface TableHeaders
    {
        TableHeaders addHeader( String title, double width );

        TableHeaders addHeader( String title, int width );

        TableHeaders addHeader( String title );

        TablePrinter start() throws IOException;
    }

    interface TablePrinter
    {
        TablePrinter print( Object... values ) throws IOException;

        void done() throws IOException;
    }
}
