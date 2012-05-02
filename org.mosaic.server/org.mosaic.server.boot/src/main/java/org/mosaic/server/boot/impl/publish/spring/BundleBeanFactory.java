package org.mosaic.server.boot.impl.publish.spring;

import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import static org.mosaic.server.boot.impl.publish.spring.SpringUtils.registerBundleBeans;

/**
 * @author arik
 */
public class BundleBeanFactory extends DefaultListableBeanFactory
{

    public BundleBeanFactory( Bundle bundle, OsgiSpringNamespacePlugin springNamespacePlugin )
    {

        // can't create a bean factory for a bundle that has no wiring
        BundleWiring wiring = bundle.adapt( BundleWiring.class );
        if( wiring == null )
        {
            throw new IllegalStateException( "Bundle '" + bundle.getSymbolicName() + "' is uninstalled!" );
        }

        // configure application context for an OSGi environment
        setAllowBeanDefinitionOverriding( false );
        setAllowCircularReferences( false );
        setBeanClassLoader( wiring.getClassLoader() );
        setCacheBeanMetadata( false );

        // add bundle beans
        registerBundleBeans( bundle, this, springNamespacePlugin );

    }
}
