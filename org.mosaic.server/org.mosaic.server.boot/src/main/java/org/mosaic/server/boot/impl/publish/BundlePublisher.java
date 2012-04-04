package org.mosaic.server.boot.impl.publish;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import org.mosaic.lifecycle.ServiceBind;
import org.mosaic.lifecycle.ServiceRef;
import org.mosaic.lifecycle.ServiceUnbind;
import org.mosaic.logging.Logger;
import org.mosaic.logging.LoggerFactory;
import org.mosaic.osgi.util.BundleUtils;
import org.mosaic.server.boot.impl.publish.requirement.*;
import org.mosaic.server.boot.impl.publish.spring.BundleClassLoader;
import org.mosaic.server.boot.impl.publish.spring.OsgiResourcePatternResolver;
import org.mosaic.server.boot.impl.publish.spring.OsgiSpringNamespacePlugin;
import org.osgi.framework.Bundle;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.UrlResource;
import org.springframework.util.ReflectionUtils;

import static org.springframework.core.GenericCollectionTypeResolver.getCollectionParameterType;

/**
 * @author arik
 */
public class BundlePublisher {

    private static final Logger LOG = LoggerFactory.getLogger( BundlePublisher.class );

    private final Bundle bundle;

    private final OsgiSpringNamespacePlugin osgiSpringNamespacePlugin;

    private Collection<ServiceRequirement> requirements;

    public BundlePublisher( Bundle bundle, OsgiSpringNamespacePlugin osgiSpringNamespacePlugin ) {
        this.bundle = bundle;
        this.osgiSpringNamespacePlugin = osgiSpringNamespacePlugin;
    }

    public void start() throws BundlePublishException {
        this.requirements = detectRequirements();
        for( ServiceRequirement requirement : this.requirements ) {
            requirement.open();
        }
        LOG.info( "Tracking bundle '{}'", BundleUtils.toString( this.bundle ) );
    }

    public void stop() {
        LOG.info( "No longer tracking bundle '{}'", BundleUtils.toString( this.bundle ) );
        for( ServiceRequirement requirement : this.requirements ) {
            requirement.close();
        }
    }

    private Collection<ServiceRequirement> detectRequirements() throws BundlePublishException {
        DefaultListableBeanFactory beanFactory = createPrototypeBeanFactory();
        Collection<ServiceRequirement> requirements = new LinkedList<>();
        for( String beanDefinitionName : beanFactory.getBeanDefinitionNames() ) {
            Class<?> beanClass = getBeanClass( beanFactory, beanDefinitionName );
            if( beanClass != null ) {
                for( Method method : ReflectionUtils.getUniqueDeclaredMethods( beanClass ) ) {
                    detectRequirementsInMethod( beanDefinitionName, method, requirements );
                }
            }
        }
        return requirements;
    }

    private DefaultListableBeanFactory createPrototypeBeanFactory() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.setAllowBeanDefinitionOverriding( false );
        beanFactory.setAllowCircularReferences( false );
        beanFactory.setAllowEagerClassLoading( true );
        beanFactory.setAllowRawInjectionDespiteWrapping( false );
        beanFactory.setBeanClassLoader( new BundleClassLoader( this.bundle ) );
        beanFactory.setCacheBeanMetadata( false );
        registerBundleBeans( beanFactory );
        return beanFactory;
    }

    private void registerBundleBeans( DefaultListableBeanFactory beanFactory ) {
        ConfigurableEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst( new MapPropertySource( "bundle", getBundleHeaders() ) );

        XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader( beanFactory );
        xmlReader.setBeanClassLoader( beanFactory.getBeanClassLoader() );
        xmlReader.setEntityResolver( this.osgiSpringNamespacePlugin );
        xmlReader.setEnvironment( environment );
        xmlReader.setNamespaceHandlerResolver( this.osgiSpringNamespacePlugin );
        xmlReader.setResourceLoader( new OsgiResourcePatternResolver( this.bundle, beanFactory.getBeanClassLoader() ) );

        Enumeration<URL> xmlFiles = this.bundle.findEntries( "/META-INF/spring/", "*.xml", true );
        while( xmlFiles.hasMoreElements() ) {
            xmlReader.loadBeanDefinitions( new UrlResource( xmlFiles.nextElement() ) );
        }
    }

    private void detectRequirementsInMethod( String beanDefinitionName,
                                             Method method,
                                             Collection<ServiceRequirement> requirements )
            throws BundlePublishException {

        ServiceRef serviceRefAnn = AnnotationUtils.findAnnotation( method, ServiceRef.class );
        if( serviceRefAnn != null ) {
            //TODO 4/4/12: support more than one parameter (service properties)
            Class<?>[] parameterTypes = method.getParameterTypes();
            if( parameterTypes.length != 1 ) {

                // @ServiceRef methods must have exactly one parameter: the service itself
                throw new BundlePublishException( "Method '" + method.getName() + "' in bean '" + beanDefinitionName + "' has the @" + ServiceRef.class.getSimpleName() + " annotation, but has an illegal number of parameters: " + parameterTypes.length );

            } else if( parameterTypes[ 0 ].isAssignableFrom( List.class ) ) {

                // add the new requirement
                requirements.add(
                        new ServiceListRequirement( this.bundle.getBundleContext(),
                                                    this,
                                                    getCollectionParameterType( new MethodParameter( method, 0 ) ),
                                                    serviceRefAnn.filter(),
                                                    beanDefinitionName,
                                                    method ) );

            } else {

                // add the new requirement
                requirements.add(
                        new ServiceRefRequirement( this.bundle.getBundleContext(),
                                                   this,
                                                   parameterTypes[ 0 ],
                                                   serviceRefAnn.filter(),
                                                   serviceRefAnn.required(),
                                                   beanDefinitionName,
                                                   method ) );

            }
        }

        ServiceBind bindAnn = AnnotationUtils.findAnnotation( method, ServiceBind.class );
        if( bindAnn != null ) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if( parameterTypes.length != 1 ) {

                // @ServiceRef methods must have exactly one parameter: the service itself
                throw new BundlePublishException( "Method '" + method.getName() + "' in bean '" + beanDefinitionName + "' has the @" + ServiceBind.class.getSimpleName() + " annotation, but has an illegal number of parameters: " + parameterTypes.length );

            } else {

                // add the new requirement
                requirements.add(
                        new ServiceBindRequirement( this.bundle.getBundleContext(),
                                                    this,
                                                    parameterTypes[ 0 ],
                                                    bindAnn.filter(),
                                                    beanDefinitionName,
                                                    method ) );

            }
        }

        ServiceUnbind unbindAnn = AnnotationUtils.findAnnotation( method, ServiceUnbind.class );
        if( bindAnn != null ) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if( parameterTypes.length != 1 ) {

                // @ServiceRef methods must have exactly one parameter: the service itself
                throw new BundlePublishException( "Method '" + method.getName() + "' in bean '" + beanDefinitionName + "' has the @" + ServiceUnbind.class.getSimpleName() + " annotation, but has an illegal number of parameters: " + parameterTypes.length );

            } else {

                // add the new requirement
                requirements.add(
                        new ServiceUnbindRequirement( this.bundle.getBundleContext(),
                                                      this,
                                                      parameterTypes[ 0 ],
                                                      unbindAnn.filter(),
                                                      beanDefinitionName,
                                                      method ) );

            }
        }
    }

    private Class<?> getBeanClass( DefaultListableBeanFactory beanFactory, String beanDefinitionName ) {
        BeanDefinition beanDefinition = beanFactory.getBeanDefinition( beanDefinitionName );
        String beanClassName = beanDefinition.getBeanClassName();
        Class<?> beanClass = null;

        // attempt to resolve class using the bean class name property of the bean definition
        if( beanClassName != null ) {
            try {
                beanClass = this.bundle.loadClass( beanClassName );
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
