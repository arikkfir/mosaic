package org.mosaic.server.boot.impl.publish.spring;

import org.osgi.framework.Bundle;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

/**
 * @author arik
 */
public class BundleBeanFactory extends DefaultListableBeanFactory {

    public BundleBeanFactory( Bundle bundle ) {
        setAllowBeanDefinitionOverriding( false );
        setAllowCircularReferences( false );
        setBeanClassLoader( new BundleClassLoader( bundle ) );
        setCacheBeanMetadata( false );
    }
}
