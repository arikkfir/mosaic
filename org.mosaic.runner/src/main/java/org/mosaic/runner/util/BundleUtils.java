package org.mosaic.runner.util;

import java.io.IOException;
import java.nio.file.Path;
import java.util.jar.JarFile;
import org.osgi.framework.Bundle;

import static java.nio.file.Files.*;

/**
 * @author arik
 */
public abstract class BundleUtils {

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

    public static String toString( Bundle bundle ) {
        if( bundle == null ) {
            return "";
        } else {
            return bundle.getSymbolicName() + "-" + bundle.getVersion() + "[" + bundle.getBundleId() + "]";
        }
    }

}
