package org.mosaic.server.boot.impl.track;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import org.mosaic.osgi.util.BundleUtils;
import org.osgi.framework.Bundle;

/**
 * @author arik
 */
public class BundleClassLoader extends ClassLoader {

    private final Bundle bundle;

    public BundleClassLoader( Bundle bundle ) {
        this.bundle = bundle;
    }

    protected Class findClass( String name ) throws ClassNotFoundException {
        try {
            return this.bundle.loadClass( name );
        } catch( ClassNotFoundException | NoClassDefFoundError e ) {
            throw new ClassNotFoundException( "Class '" + name + "' could not be found from bundle '" + BundleUtils.toString( this.bundle ) + "'", e );
        }
    }

    protected URL findResource( String name ) {
        return this.bundle.getResource( name );
    }

    protected Enumeration<URL> findResources( String name ) throws IOException {
        return this.bundle.getResources( name );
    }

    public URL getResource( String name ) {
        return findResource( name );
    }

    protected Class loadClass( String name, boolean resolve ) throws ClassNotFoundException {
        Class clazz = findClass( name );
        if( resolve ) {
            resolveClass( clazz );
        }
        return clazz;
    }

    public String toString() {
        return getClass().getSimpleName() + "[bundle=" + BundleUtils.toString( this.bundle ) + "]";
    }
}
