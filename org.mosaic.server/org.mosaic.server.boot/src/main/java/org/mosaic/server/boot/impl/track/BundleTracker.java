package org.mosaic.server.boot.impl.track;

import java.net.URL;
import java.util.Enumeration;
import org.osgi.framework.Bundle;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.UrlResource;

/**
 * @author arik
 */
public class BundleTracker {

    private final Bundle bundle;

    private final OsgiSpringNamespacePlugin osgiSpringNamespacePlugin;

    private ConfigurableEnvironment environment;

    private OsgiConversionService conversionService;

    private GenericApplicationContext applicationContext;

    public BundleTracker( Bundle bundle, OsgiSpringNamespacePlugin osgiSpringNamespacePlugin ) {
        this.bundle = bundle;
        this.osgiSpringNamespacePlugin = osgiSpringNamespacePlugin;
    }

    public void start() {
        this.environment = new BundleEnvironment( this.bundle );
        this.conversionService = new OsgiConversionService( this.bundle );
        this.conversionService.open();

        DefaultListableBeanFactory beanFactory = createPrototypeBeanFactory();
        registerBundleBeans( beanFactory );
        //TODO 4/3/12: discover hooks and dependencies
        //TODO 4/3/12: open all dependency trackers - LET THEM CREATE APPCTX WHEN DUE, WHY THREAD??
    }

    public void stop() {
        //TODO 4/3/12: close all dependencies and trackers

        this.conversionService.close();
        this.conversionService = null;
        this.environment = null;
    }

    private void registerBundleBeans( DefaultListableBeanFactory beanFactory ) {
        XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader( beanFactory );
        xmlReader.setBeanClassLoader( beanFactory.getBeanClassLoader() );
        xmlReader.setEntityResolver( this.osgiSpringNamespacePlugin );
        xmlReader.setEnvironment( this.environment );
        xmlReader.setNamespaceHandlerResolver( this.osgiSpringNamespacePlugin );

        Enumeration<URL> xmlFiles = this.bundle.findEntries( "/META-INF/spring/", "*.xml", true );
        while( xmlFiles.hasMoreElements() ) {
            xmlReader.loadBeanDefinitions( new UrlResource( xmlFiles.nextElement() ) );
        }
    }

    private DefaultListableBeanFactory createPrototypeBeanFactory() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.setAllowBeanDefinitionOverriding( false );
        beanFactory.setAllowCircularReferences( false );
        beanFactory.setAllowEagerClassLoading( true );
        beanFactory.setAllowRawInjectionDespiteWrapping( false );
        beanFactory.setBeanClassLoader( new BundleClassLoader( this.bundle ) );
        beanFactory.setCacheBeanMetadata( false );
        beanFactory.setConversionService( this.conversionService );
        return beanFactory;
    }
}
