package org.mosaic.server.shell.impl.util;

import java.text.BreakIterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author arik
 */
public abstract class StringUtils {

    public static List<String> splitLinesOnLengthAndWords( String text, int length ) {
        List<String> lines = new LinkedList<>();
        if( text == null || text.length() == 0 ) {
            lines.add( "" );
            return lines;
        }

        for( String line : text.split( "\n" ) ) {

            StringBuilder lineBuffer = new StringBuilder( line.length() );

            BreakIterator wordIterator = BreakIterator.getWordInstance();
            wordIterator.setText( line );
            int start = wordIterator.first();
            for( int end = wordIterator.next(); end != BreakIterator.DONE; start = end, end = wordIterator.next() ) {
                for( String word : splitLinesOnLength( line.substring( start, end ), length ) ) {

                    if( lineBuffer.length() + word.length() > length ) {
                        lines.add( lineBuffer.toString() );
                        lineBuffer.delete( 0, Integer.MAX_VALUE );
                    }
                    lineBuffer.append( word );

                }
            }

            if( lineBuffer.length() > 0 ) {
                lines.add( lineBuffer.toString() );
            }

        }

        return lines;
    }

    public static List<String> splitLinesOnLength( String text, int length ) {
        List<String> lines = new LinkedList<>();
        if( text == null || text.length() == 0 ) {
            lines.add( "" );
            return lines;
        }

        StringBuilder lineBuffer = new StringBuilder( text.length() );
        for( int ci = 0; ci < text.length(); ci++ ) {
            char c = text.charAt( ci );
            if( c == '\n' || c == '\r' ) {

                lines.add( lineBuffer.toString() );
                lineBuffer.delete( 0, Integer.MAX_VALUE );

            } else if( lineBuffer.length() < length ) {

                lineBuffer.append( c );

            } else {

                lines.add( lineBuffer.toString() );
                lineBuffer.delete( 0, Integer.MAX_VALUE );

            }
        }

        if( lineBuffer.length() > 0 ) {
            lines.add( lineBuffer.toString() );
        }

        return lines;
    }

    public static String repeat( char c, int length ) {
        StringBuilder buf = new StringBuilder();
        for( int i = 0; i < length; i++ ) {
            buf.append( c );
        }
        return buf.toString();
    }

    public static String rightPad( String text, int length ) {
        while( text.length() < length ) {
            text += ' ';
        }
        return text;
    }

    public static String leftPad( Object value, int length ) {
        String text = value.toString();
        while( text.length() < length ) {
            text = ' ' + text;
        }
        return text;
    }

}
