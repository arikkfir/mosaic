package org.mosaic.server.boot.impl.publish.requirement;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.mosaic.lifecycle.ServiceBind;
import org.mosaic.lifecycle.ServiceRef;
import org.mosaic.lifecycle.ServiceUnbind;
import org.mosaic.server.boot.impl.publish.BundlePublisher;
import org.mosaic.server.boot.impl.publish.spring.BeanFactoryUtils;
import org.mosaic.server.boot.impl.publish.spring.BundleBeanFactory;
import org.mosaic.server.boot.impl.publish.spring.OsgiSpringNamespacePlugin;
import org.osgi.framework.Bundle;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

import static org.mosaic.server.boot.impl.publish.spring.BeanFactoryUtils.registerBundleBeans;
import static org.springframework.core.GenericCollectionTypeResolver.getCollectionParameterType;

/**
 * @author arik
 */
public class RequirementFactory {

    private final BundlePublisher publisher;

    private final Bundle bundle;

    private final OsgiSpringNamespacePlugin osgiSpringNamespacePlugin;

    public RequirementFactory( BundlePublisher publisher,
                               Bundle bundle,
                               OsgiSpringNamespacePlugin osgiSpringNamespacePlugin ) {
        this.publisher = publisher;
        this.bundle = bundle;
        this.osgiSpringNamespacePlugin = osgiSpringNamespacePlugin;
    }

    public Collection<Requirement> detectRequirements() {
        BundleBeanFactory beanFactory = new BundleBeanFactory( this.bundle );
        registerBundleBeans( this.bundle, beanFactory, beanFactory.getBeanClassLoader(), this.osgiSpringNamespacePlugin );

        Collection<Requirement> requirements = new LinkedList<>();
        for( String beanDefinitionName : beanFactory.getBeanDefinitionNames() ) {
            Class<?> beanClass = BeanFactoryUtils.getBeanClass( this.bundle, beanFactory, beanDefinitionName );
            if( beanClass != null ) {
                for( Method method : ReflectionUtils.getUniqueDeclaredMethods( beanClass ) ) {
                    detectRequirementsInMethod( beanDefinitionName, method, requirements );
                }
            }
        }
        return requirements;
    }

    private void detectRequirementsInMethod( String beanDefinitionName,
                                             Method method,
                                             Collection<Requirement> requirements ) {

        ServiceRef serviceRefAnn = AnnotationUtils.findAnnotation( method, ServiceRef.class );
        if( serviceRefAnn != null ) {
            //TODO 4/4/12: support more than one parameter (service properties)
            Class<?>[] parameterTypes = method.getParameterTypes();
            if( parameterTypes.length != 1 ) {

                // @ServiceRef methods must have exactly one parameter: the service itself
                throw new IllegalStateException( "Method '" + method.getName() + "' in bean '" + beanDefinitionName + "' has the @" + ServiceRef.class.getSimpleName() + " annotation, but has an illegal number of parameters: " + parameterTypes.length );

            } else if( parameterTypes[ 0 ].isAssignableFrom( List.class ) ) {

                // add the new requirement
                requirements.add(
                        new ServiceListRequirement(
                                this.publisher,
                                getCollectionParameterType( new MethodParameter( method, 0 ) ),
                                serviceRefAnn.filter(),
                                beanDefinitionName,
                                method ) );

            } else {

                // add the new requirement
                requirements.add(
                        new ServiceRefRequirement(
                                this.publisher,
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
                throw new IllegalStateException( "Method '" + method.getName() + "' in bean '" + beanDefinitionName + "' has the @" + ServiceBind.class.getSimpleName() + " annotation, but has an illegal number of parameters: " + parameterTypes.length );

            } else {

                // add the new requirement
                requirements.add(
                        new ServiceBindRequirement(
                                this.publisher,
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
                throw new IllegalStateException( "Method '" + method.getName() + "' in bean '" + beanDefinitionName + "' has the @" + ServiceUnbind.class.getSimpleName() + " annotation, but has an illegal number of parameters: " + parameterTypes.length );

            } else {

                // add the new requirement
                requirements.add(
                        new ServiceUnbindRequirement(
                                this.publisher,
                                parameterTypes[ 0 ],
                                unbindAnn.filter(),
                                beanDefinitionName,
                                method ) );

            }
        }
    }
}
