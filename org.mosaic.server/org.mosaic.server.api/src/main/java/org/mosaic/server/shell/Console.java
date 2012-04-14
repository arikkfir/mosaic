package org.mosaic.server.shell;

import java.io.IOException;
import java.io.Writer;

/**
 * @author arik
 */
public interface Console {

    interface TableHeaders {

        TableHeaders addHeader( String title, int width );

        TablePrinter start() throws IOException;

    }

    interface TablePrinter {

        TablePrinter print( Object... values ) throws IOException;

        void done() throws IOException;

    }

    TableHeaders createTable();

    TableHeaders createTable( int indent );

    Console printStackTrace( Throwable throwable ) throws IOException;

    @SuppressWarnings( "UnusedDeclaration" )
    Console printStackTrace( String message, Throwable throwable ) throws IOException;

    Writer getWriter();

    @SuppressWarnings( "UnusedDeclaration" )
    int getWidth();

    @SuppressWarnings( "UnusedDeclaration" )
    int getHeight();

    @SuppressWarnings( "UnusedDeclaration" )
    boolean setCursorPosition( int position ) throws IOException;

    Console flush() throws IOException;

    @SuppressWarnings( "UnusedDeclaration" )
    boolean backspace() throws IOException;

    @SuppressWarnings( "UnusedDeclaration" )
    int moveCursor( int num ) throws IOException;

    @SuppressWarnings( "UnusedDeclaration" )
    boolean replace( int num, String replacement ) throws IOException;

    int readCharacter() throws IOException;

    int readCharacter( char... allowed ) throws IOException;

    int ask( String question, char... allowed ) throws IOException;

    String readLine() throws IOException;

    Console print( Object n ) throws IOException;

    Console println() throws IOException;

    Console println( Object n ) throws IOException;

    @SuppressWarnings( "UnusedDeclaration" )
    boolean delete() throws IOException;

    @SuppressWarnings( "UnusedDeclaration" )
    boolean killLine() throws IOException;

    @SuppressWarnings( "UnusedDeclaration" )
    boolean clearScreen() throws IOException;

    @SuppressWarnings( "UnusedDeclaration" )
    Console beep() throws IOException;

    @SuppressWarnings( "UnusedDeclaration" )
    boolean paste() throws IOException;

}
