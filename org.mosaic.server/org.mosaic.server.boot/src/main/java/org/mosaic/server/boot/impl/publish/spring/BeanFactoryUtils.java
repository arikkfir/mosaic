package org.mosaic.server.boot.impl.publish.spring;

import java.net.URL;
import java.util.Enumeration;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.UrlResource;

/**
 * @author arik
 */
public abstract class BeanFactoryUtils {

    public static void registerBundleBeans( Bundle bundle,
                                            BeanDefinitionRegistry beanFactory,
                                            OsgiSpringNamespacePlugin osgiSpringNamespacePlugin ) {
        BundleWiring wiring = bundle.adapt( BundleWiring.class );
        if( wiring == null ) {
            throw new IllegalArgumentException( "Bundle has no wiring!" );
        }

        XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader( beanFactory );
        xmlReader.setBeanClassLoader( wiring.getClassLoader() );
        xmlReader.setEntityResolver( osgiSpringNamespacePlugin );
        xmlReader.setEnvironment( new BundleEnvironment( bundle ) );
        xmlReader.setNamespaceHandlerResolver( osgiSpringNamespacePlugin );
        xmlReader.setResourceLoader( new OsgiResourcePatternResolver( bundle, wiring.getClassLoader() ) );

        Enumeration<URL> xmlFiles = bundle.findEntries( "/META-INF/spring/", "*.xml", true );
        while( xmlFiles.hasMoreElements() ) {
            xmlReader.loadBeanDefinitions( new UrlResource( xmlFiles.nextElement() ) );
        }
    }

    public static Class<?> getBeanClass( Bundle bundle,
                                         BeanDefinitionRegistry beanFactory,
                                         String beanDefinitionName ) {
        BeanDefinition beanDefinition = beanFactory.getBeanDefinition( beanDefinitionName );
        String beanClassName = beanDefinition.getBeanClassName();
        Class<?> beanClass = null;

        // attempt to resolve class using the bean class name property of the bean definition
        if( beanClassName != null ) {
            try {
                beanClass = bundle.loadClass( beanClassName );
            } catch( ClassNotFoundException ignore ) {
            }
        }

        // if failed, see if we can cast the bean definition to AbstractBeanDefinition and extract it from there
        if( beanClass == null && beanDefinition instanceof AbstractBeanDefinition ) {
            AbstractBeanDefinition def = ( AbstractBeanDefinition ) beanDefinition;
            beanClass = def.getBeanClass();
        }
        return beanClass;
    }

}
