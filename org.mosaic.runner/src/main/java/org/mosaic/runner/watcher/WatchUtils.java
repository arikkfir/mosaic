package org.mosaic.runner.watcher;

import java.io.IOException;
import java.nio.file.Path;
import java.util.jar.JarFile;

import static java.nio.file.Files.*;

/**
 * @author arik
 */
public abstract class WatchUtils {

    public static boolean isBundle( Path path ) {
        try {
            if( !isDirectory( path ) && isRegularFile( path ) && isReadable( path ) ) {
                if( path.getFileName().toString().toLowerCase().endsWith( ".jar" ) ) {
                    JarFile jarFile = new JarFile( path.toFile(), true, JarFile.OPEN_READ );
                    return jarFile.getManifest().getMainAttributes().getValue( "Bundle-SymbolicName" ) != null;
                }
            }
        } catch( IOException e ) {
            //no-op
        }
        return false;
    }
}
