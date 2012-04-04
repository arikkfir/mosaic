package org.mosaic.server.boot.impl.publish.spring;

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

    @Override
    protected Class<?> findClass( String name ) throws ClassNotFoundException {
        try {
            return this.bundle.loadClass( name );
        } catch( ClassNotFoundException | NoClassDefFoundError e ) {
            throw new ClassNotFoundException( "Class '" + name + "' could not be found from bundle '" + BundleUtils.toString( this.bundle ) + "'", e );
        }
    }

    @Override
    protected URL findResource( String name ) {
        return this.bundle.getResource( name );
    }

    @Override
    protected Enumeration<URL> findResources( String name ) throws IOException {
        return this.bundle.getResources( name );
    }

    @Override
    public URL getResource( String name ) {
        return this.bundle.getResource( name );
    }

    @Override
    public Enumeration<URL> getResources( String name ) throws IOException {
        return this.bundle.getResources( name );
    }

    @Override
    public Class<?> loadClass( String name ) throws ClassNotFoundException {
        try {
            return this.bundle.loadClass( name );
        } catch( ClassNotFoundException | NoClassDefFoundError e ) {
            throw new ClassNotFoundException( "Class '" + name + "' could not be found from bundle '" + BundleUtils.toString( this.bundle ) + "'", e );
        }
    }

    @Override
    protected Class<?> loadClass( String name, boolean resolve ) throws ClassNotFoundException {
        try {
            Class<?> clazz = this.bundle.loadClass( name );
            if( resolve ) {
                resolveClass( clazz );
            }
            return clazz;
        } catch( ClassNotFoundException | NoClassDefFoundError e ) {
            throw new ClassNotFoundException( "Class '" + name + "' could not be found from bundle '" + BundleUtils.toString( this.bundle ) + "'", e );
        }
    }

    public String toString() {
        return getClass().getSimpleName() + "[bundle=" + BundleUtils.toString( this.bundle ) + "]";
    }
}
