package org.mosaic.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.lifecycle.impl.ModuleManagerImpl;
import org.mosaic.lifecycle.impl.util.BundleUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static java.nio.file.Files.copy;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * @author arik
 */
public class MosaicActivator implements BundleActivator
{
    @Nullable
    private static BundleContext bundleContext;

    @Nullable
    public static BundleContext getBundleContext()
    {
        return bundleContext;
    }

    @Nullable
    private ClassPathXmlApplicationContext applicationContext;

    @Override
    public void start( @Nonnull BundleContext bundleContext ) throws Exception
    {
        MosaicActivator.bundleContext = bundleContext;

        deployEmbeddedBundle( "jcl-over-slf4j" );
        deployEmbeddedBundle( "log4j-over-slf4j" );

        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext();
        applicationContext.setConfigLocation( "/lifecycle-beans.xml" );
        applicationContext.setClassLoader( BundleUtils.getClassLoader( bundleContext ) );
        applicationContext.setDisplayName( "Mosaic Lifecycle" );
        applicationContext.setAllowBeanDefinitionOverriding( false );
        applicationContext.setAllowCircularReferences( false );
        applicationContext.setId( "MosaicLifecycle" );
        applicationContext.refresh();
        this.applicationContext = applicationContext;
        this.applicationContext.getBean( ModuleManagerImpl.class ).start();
    }

    @Override
    public void stop( @Nonnull BundleContext bundleContext ) throws Exception
    {
        if( this.applicationContext != null )
        {
            this.applicationContext.close();
            this.applicationContext = null;
        }
        MosaicActivator.bundleContext = null;
    }

    private void deployEmbeddedBundle( @Nonnull String name ) throws IOException, BundleException
    {
        BundleContext bc = bundleContext;
        if( bc == null )
        {
            throw new IllegalStateException( "Bundle context has not been set" );
        }

        Path bundleFile = Files.createTempFile( name, ".jar" );
        try( InputStream in = findEmbeddedBundle( name ).openStream() )
        {
            copy( in, bundleFile, REPLACE_EXISTING );
        }

        bc.installBundle( bundleFile.toUri().toString() ).start();
    }

    @Nonnull
    private URL findEmbeddedBundle( @Nonnull String name )
    {
        BundleContext bc = bundleContext;
        if( bc == null )
        {
            throw new IllegalStateException( "Bundle context has not been set" );
        }

        Bundle bundle = bc.getBundle();
        Enumeration<URL> jcl = bundle.findEntries( "/", name + "-*.jar", false );
        if( jcl.hasMoreElements() )
        {
            return jcl.nextElement();
        }
        else
        {
            throw new IllegalStateException( "Could not find embedded bundle '" + name + "' in lifecycle bundle" );
        }
    }
}
