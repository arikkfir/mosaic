package org.mosaic.server.boot.impl.track;

import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import org.mosaic.logging.Logger;
import org.mosaic.logging.LoggerFactory;
import org.mosaic.osgi.util.BundleUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.UrlResource;

/**
 * @author arik
 */
public class BundleTracker {

    private static final Logger LOG = LoggerFactory.getLogger( BundleTracker.class );

    private final BundleContext bundleContext;

    private final Bundle bundle;

    private final OsgiSpringNamespacePlugin osgiSpringNamespacePlugin;

    private final OsgiConversionService conversionService;

    private ConfigurableEnvironment environment;

    private GenericApplicationContext applicationContext;

    public BundleTracker( BundleContext bundleContext,
                          Bundle bundle,
                          OsgiSpringNamespacePlugin springNamespacePlugin,
                          OsgiConversionService conversionService ) {
        this.bundleContext = bundleContext;
        this.bundle = bundle;
        this.osgiSpringNamespacePlugin = springNamespacePlugin;
        this.conversionService = conversionService;
    }

    public void start() {
        LOG.info( "Tracking bundle '{}'", BundleUtils.toString( this.bundle ) );

        this.environment = new StandardEnvironment();
        this.environment.getPropertySources().addFirst( new MapPropertySource( "bundle", getBundleHeaders() ) );

        DefaultListableBeanFactory beanFactory = createPrototypeBeanFactory();
        registerBundleBeans( beanFactory );
        //TODO 4/3/12: discover hooks and dependencies
        //TODO 4/3/12: open all dependency trackers - LET THEM CREATE APPCTX WHEN DUE, WHY THREAD??
    }

    public void stop() {
        //TODO 4/3/12: close all dependencies and trackers

        this.environment.getPropertySources().remove( "bundle" );
        this.environment = null;

        LOG.info( "Stopped tracking bundle '{}'", BundleUtils.toString( this.bundle ) );
    }

    private void registerBundleBeans( DefaultListableBeanFactory beanFactory ) {
        XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader( beanFactory );
        xmlReader.setBeanClassLoader( beanFactory.getBeanClassLoader() );
        xmlReader.setEntityResolver( this.osgiSpringNamespacePlugin );
        xmlReader.setEnvironment( this.environment );
        xmlReader.setNamespaceHandlerResolver( this.osgiSpringNamespacePlugin );
        xmlReader.setResourceLoader( new OsgiResourcePatternResolver( this.bundleContext, beanFactory.getBeanClassLoader() ) );

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

    private Map<String, Object> getBundleHeaders() {
        Map<String, Object> headersMap = new HashMap<>();
        Dictionary<String, String> headers = this.bundle.getHeaders();
        Enumeration<String> headerNames = headers.keys();
        while( headerNames.hasMoreElements() ) {
            String headerName = headerNames.nextElement();
            headersMap.put( headerName, headers.get( headerName ) );
        }
        return headersMap;
    }
}
