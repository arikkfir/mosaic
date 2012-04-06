package org.mosaic.server.boot.impl.publish.requirement;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.mosaic.lifecycle.*;
import org.mosaic.server.boot.impl.publish.BundlePublisher;
import org.mosaic.server.boot.impl.publish.spring.BeanFactoryUtils;
import org.mosaic.server.boot.impl.publish.spring.BundleBeanFactory;
import org.mosaic.server.boot.impl.publish.spring.OsgiSpringNamespacePlugin;
import org.osgi.framework.Bundle;
import org.springframework.core.MethodParameter;
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

                // detect service classes for registration
                ServiceExport exportAnn = beanClass.getAnnotation( ServiceExport.class );
                if( exportAnn != null ) {
                    requirements.add( new ServiceExportRequirement( this.publisher, beanDefinitionName, exportAnn.value() ) );
                }

                // detect method requirements
                for( Method method : ReflectionUtils.getUniqueDeclaredMethods( beanClass ) ) {
                    detectServiceRef( beanDefinitionName, method, requirements );
                    detectServiceBind( beanDefinitionName, method, requirements );
                    detectServiceUnbind( beanDefinitionName, method, requirements );
                    detectMethodEndpoint( beanDefinitionName, method, requirements );
                }
            }
        }
        return requirements;
    }

    private void detectServiceRef( String beanDefinitionName, Method method, Collection<Requirement> requirements ) {
        MetaAnnotation<ServiceRef> serviceRefAnn = findMetaAnnotation( method, ServiceRef.class );
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
                                serviceRefAnn.metaAnnotation.filter(),
                                beanDefinitionName,
                                method ) );

            } else {

                // add the new requirement
                requirements.add(
                        new ServiceRefRequirement(
                                this.publisher,
                                parameterTypes[ 0 ],
                                serviceRefAnn.metaAnnotation.filter(),
                                serviceRefAnn.metaAnnotation.required(),
                                beanDefinitionName,
                                method ) );

            }
        }
    }

    private void detectServiceBind( String beanDefinitionName, Method method, Collection<Requirement> requirements ) {
        MetaAnnotation<ServiceBind> bindAnn = findMetaAnnotation( method, ServiceBind.class );
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
                                bindAnn.metaAnnotation.filter(),
                                beanDefinitionName,
                                method ) );

            }
        }
    }

    private void detectServiceUnbind( String beanDefinitionName, Method method, Collection<Requirement> requirements ) {
        MetaAnnotation<ServiceUnbind> unbindAnn = findMetaAnnotation( method, ServiceUnbind.class );
        if( unbindAnn != null ) {
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
                                unbindAnn.metaAnnotation.filter(),
                                beanDefinitionName,
                                method ) );

            }
        }
    }

    private void detectMethodEndpoint( String beanDefinitionName,
                                       Method method,
                                       Collection<Requirement> requirements ) {

        MetaAnnotation<MethodEndpointMarker> metaAnnotation = findMetaAnnotation( method, MethodEndpointMarker.class );
        if( metaAnnotation != null ) {
            AnnotatedElement container = metaAnnotation.container;
            if( container instanceof Class ) {
                Class containerClass = ( Class ) container;
                requirements.add( new MethodEndpointRequirement(
                        this.publisher, beanDefinitionName, method, containerClass ) );
            }
        }
    }

    private static <T extends Annotation> MetaAnnotation<T> findMetaAnnotation(
            AnnotatedElement annotatedElement, Class<T> metaAnnotationType ) {

        if( annotatedElement instanceof Class && annotatedElement.equals( metaAnnotationType ) ) {
            return null;
        }

        T metaAnnotation = annotatedElement.getAnnotation( metaAnnotationType );
        if( metaAnnotation != null ) {
            return new MetaAnnotation<>( metaAnnotation, annotatedElement );
        }

        for( Annotation annotation : annotatedElement.getAnnotations() ) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            String pkgName = annotationType.getPackage().getName();
            if( !pkgName.startsWith( "java.lang.annotation" ) && !annotationType.equals( annotatedElement ) ) {
                MetaAnnotation<T> meta = findMetaAnnotation( annotationType, metaAnnotationType );
                if( meta != null ) {
                    return meta;
                }
            }
        }
        return null;
    }

    private static class MetaAnnotation<T extends Annotation> {

        private final T metaAnnotation;

        private final AnnotatedElement container;

        private MetaAnnotation( T metaAnnotation, AnnotatedElement container ) {
            this.metaAnnotation = metaAnnotation;
            this.container = container;
        }
    }
}
