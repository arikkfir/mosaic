package org.mosaic.util.weaving.impl;

import java.io.InputStream;
import java.net.URL;
import javassist.ClassPath;
import javassist.NotFoundException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.weaving.spi.WeaverSpi;
import org.osgi.framework.wiring.BundleWiring;

/**
 * @author arik
 */
public class WeavingClassPath implements ClassPath
{
    @Nonnull
    private final BundleWiring bundleWiring;

    public WeavingClassPath( @Nonnull BundleWiring bundleWiring )
    {
        this.bundleWiring = bundleWiring;
    }

    @Nullable
    @Override
    public InputStream openClassfile( @Nonnull String classname ) throws NotFoundException
    {
        ClassLoader classLoader;
        if( "org.mosaic.util.weaving.spi.WeaverSpi".equals( classname ) )
        {
            classLoader = InterceptionWeavingHook.class.getClassLoader();
        }
        else
        {
            classLoader = bundleWiring.getClassLoader();
        }
        String fileName = classname.replace( '.', '/' ) + ".class";
        return classLoader.getResourceAsStream( fileName );
    }

    @Nullable
    @Override
    public URL find( @Nonnull String classname )
    {
        ClassLoader classLoader;
        if( WeaverSpi.class.getName().equals( classname ) )
        {
            classLoader = InterceptionWeavingHook.class.getClassLoader();
        }
        else
        {
            classLoader = bundleWiring.getClassLoader();
        }
        String fileName = classname.replace( '.', '/' ) + ".class";
        return classLoader.getResource( fileName );
    }

    @Override
    public void close()
    {
        // no-op
    }
}
