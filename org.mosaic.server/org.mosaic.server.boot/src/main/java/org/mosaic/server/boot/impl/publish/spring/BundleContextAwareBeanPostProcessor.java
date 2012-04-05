package org.mosaic.server.boot.impl.publish.spring;

import org.mosaic.lifecycle.BundleContextAware;
import org.osgi.framework.BundleContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * @author arik
 */
public class BundleContextAwareBeanPostProcessor implements BeanPostProcessor {

    private final BundleContext bundleContext;

    public BundleContextAwareBeanPostProcessor( BundleContext bundleContext ) {
        this.bundleContext = bundleContext;
    }

    @Override
    public Object postProcessBeforeInitialization( Object bean, String beanName ) throws BeansException {
        if( bean instanceof BundleContextAware ) {
            BundleContextAware bundleContextAware = ( BundleContextAware ) bean;
            bundleContextAware.setBundleContext( this.bundleContext );
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization( Object bean, String beanName ) throws BeansException {
        return bean;
    }
}
