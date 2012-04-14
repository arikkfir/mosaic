package org.mosaic.server.boot.impl.publish.spring;

import org.mosaic.osgi.util.BundleUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;
import org.springframework.context.support.GenericApplicationContext;

/**
 * @author arik
 */
public class BundleApplicationContext extends GenericApplicationContext {

    public BundleApplicationContext( Bundle bundle ) {
        BundleWiring wiring = bundle.adapt( BundleWiring.class );
        if( wiring == null ) {
            throw new IllegalStateException( "Bundle '" + bundle.getSymbolicName() + "' is uninstalled!" );
        }


        setAllowBeanDefinitionOverriding( false );
        setAllowCircularReferences( false );
        setClassLoader( wiring.getClassLoader() );
        setDisplayName( "ApplicationContext[" + BundleUtils.toString( bundle ) + "]" );
        setEnvironment( new BundleEnvironment( bundle ) );
        setId( BundleUtils.toString( bundle ) );
        setResourceLoader( new OsgiResourcePatternResolver( bundle, getClassLoader() ) );
        getBeanFactory().addBeanPostProcessor( new BundleContextAwareBeanPostProcessor( bundle.getBundleContext() ) );
    }

}
