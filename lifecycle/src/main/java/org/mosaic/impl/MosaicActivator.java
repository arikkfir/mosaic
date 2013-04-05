package org.mosaic.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.lifecycle.impl.util.BundleUtils;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

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

        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext();
        applicationContext.setConfigLocation( "/lifecycle-beans.xml" );
        applicationContext.setClassLoader( BundleUtils.getClassLoader( bundleContext ) );
        applicationContext.setDisplayName( "Mosaic Lifecycle" );
        applicationContext.setAllowBeanDefinitionOverriding( false );
        applicationContext.setAllowCircularReferences( false );
        applicationContext.setId( "MosaicLifecycle" );
        applicationContext.refresh();
        this.applicationContext = applicationContext;
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
}
