package org.mosaic.server.shell;

import java.io.IOException;
import java.io.Writer;

/**
 * @author arik
 */
@SuppressWarnings( "UnusedDeclaration" )
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

    Writer getWriter();

    int getWidth();

    int getHeight();

    boolean setCursorPosition( int position ) throws IOException;

    Console flush() throws IOException;

    boolean backspace() throws IOException;

    int moveCursor( int num ) throws IOException;

    boolean replace( int num, String replacement );

    int readCharacter() throws IOException;

    int readCharacter( char... allowed ) throws IOException;

    String readLine() throws IOException;

    String readLine( Character mask ) throws IOException;

    String readLine( String prompt ) throws IOException;

    String readLine( String prompt, Character mask ) throws IOException;

    Console print( CharSequence s ) throws IOException;

    Console println( CharSequence s ) throws IOException;

    Console println() throws IOException;

    boolean delete() throws IOException;

    boolean killLine() throws IOException;

    boolean clearScreen() throws IOException;

    Console beep() throws IOException;

    boolean paste() throws IOException;

    Console resetPromptLine( String prompt, String buffer, int cursorDest ) throws IOException;

    Console printSearchStatus( String searchTerm, String match ) throws IOException;

    Console restoreLine( String originalPrompt, int cursorDest ) throws IOException;

    int searchBackwards( String searchTerm, int startIndex );

    int searchBackwards( String searchTerm );

    int searchBackwards( String searchTerm, int startIndex, boolean startsWith );
}
