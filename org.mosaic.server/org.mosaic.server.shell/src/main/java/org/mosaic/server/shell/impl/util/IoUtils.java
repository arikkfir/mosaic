package org.mosaic.server.shell.impl.util;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;

/**
 * @author arik
 */
public abstract class IoUtils {

    public static void flush( Flushable... flushables ) {
        for( Flushable flushable : flushables ) {
            try {
                flushable.flush();
            } catch( IOException ignore ) {
            }
        }
    }

    public static void close( Closeable... closeables ) {
        for( Closeable closeable : closeables ) {
            try {
                closeable.close();
            } catch( IOException ignore ) {
            }
        }
    }

}
