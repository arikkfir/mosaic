package org.mosaic.server.boot.impl.publish.spring;

import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

/**
 * @author arik
 */
public class BundleBeanFactory extends DefaultListableBeanFactory {

    public BundleBeanFactory( Bundle bundle ) {
        BundleWiring wiring = bundle.adapt( BundleWiring.class );
        if( wiring == null ) {
            throw new IllegalStateException( "Bundle '" + bundle.getSymbolicName() + "' is uninstalled!" );
        }

        setAllowBeanDefinitionOverriding( false );
        setAllowCircularReferences( false );
        setBeanClassLoader( wiring.getClassLoader() );
        setCacheBeanMetadata( false );
    }
}
