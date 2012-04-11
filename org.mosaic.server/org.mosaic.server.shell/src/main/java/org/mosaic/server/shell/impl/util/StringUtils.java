package org.mosaic.server.shell.impl.util;

/**
 * @author arik
 */
public abstract class StringUtils {

    public static String repeat( String text, int length ) {
        StringBuilder buf = new StringBuilder();
        for( int i = 0; i < length; i++ ) {
            buf.append( text );
        }
        return buf.toString();
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
