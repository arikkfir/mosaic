package org.mosaic.server.boot.impl.publish.requirement;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import org.mosaic.lifecycle.*;
import org.mosaic.server.boot.impl.publish.BundleTracker;
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

    private final BundleTracker tracker;

    private final Bundle bundle;

    private final OsgiSpringNamespacePlugin osgiSpringNamespacePlugin;

    public RequirementFactory( BundleTracker tracker,
                               Bundle bundle,
                               OsgiSpringNamespacePlugin osgiSpringNamespacePlugin ) {
        this.tracker = tracker;
        this.bundle = bundle;
        this.osgiSpringNamespacePlugin = osgiSpringNamespacePlugin;
    }

    public Collection<Requirement> detectRequirements() {
        BundleBeanFactory beanFactory = new BundleBeanFactory( this.bundle );
        registerBundleBeans( this.bundle, beanFactory, beanFactory.getBeanClassLoader(), this.osgiSpringNamespacePlugin );

        List<Requirement> requirements = new LinkedList<>();
        for( String beanDefinitionName : beanFactory.getBeanDefinitionNames() ) {
            Class<?> beanClass = BeanFactoryUtils.getBeanClass( this.bundle, beanFactory, beanDefinitionName );
            if( beanClass != null ) {

                // detect service classes for registration
                ServiceExport exportAnn = beanClass.getAnnotation( ServiceExport.class );
                if( exportAnn != null ) {
                    requirements.add( new ServiceExportRequirement( this.tracker, beanDefinitionName, exportAnn.value() ) );
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
        Collections.sort( requirements, new RequirementPriorityComparator() );
        return requirements;
    }

    private void detectServiceRef( String beanDefinitionName, Method method, Collection<Requirement> requirements ) {
        ServiceRef serviceRefAnn = findAnnotation( method, ServiceRef.class );
        if( serviceRefAnn != null ) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if( parameterTypes.length == 0 ) {

                // @ServiceRef methods must have at least one parameter
                throw new IllegalStateException( "Method '" + method.getName() + "' in bean '" + beanDefinitionName + "' has no parameters, but has the @" + ServiceRef.class.getSimpleName() + " annotation" );

            } else if( parameterTypes[ 0 ].isAssignableFrom( List.class ) ) {

                // add the new requirement
                requirements.add(
                        new ServiceListRequirement(
                                this.tracker,
                                getCollectionParameterType( new MethodParameter( method, 0 ) ),
                                serviceRefAnn.filter(),
                                beanDefinitionName,
                                method ) );

            } else {

                // add the new requirement
                requirements.add(
                        new ServiceRefRequirement(
                                this.tracker,
                                parameterTypes[ 0 ],
                                serviceRefAnn.filter(),
                                serviceRefAnn.required(),
                                beanDefinitionName,
                                method ) );

            }
        }
    }

    private void detectServiceBind( String beanDefinitionName, Method method, Collection<Requirement> requirements ) {
        ServiceBind bindAnn = findAnnotation( method, ServiceBind.class );
        if( bindAnn != null ) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if( parameterTypes.length == 0 ) {

                // @ServiceBind methods must have at least one parameter
                throw new IllegalStateException( "Method '" + method.getName() + "' in bean '" + beanDefinitionName + "' has no parameters, but has the @" + ServiceBind.class.getSimpleName() + " annotation" );

            } else {

                // add the new requirement
                requirements.add(
                        new ServiceBindRequirement(
                                this.tracker,
                                parameterTypes[ 0 ],
                                bindAnn.filter(),
                                beanDefinitionName,
                                method ) );

            }
        }
    }

    private void detectServiceUnbind( String beanDefinitionName, Method method, Collection<Requirement> requirements ) {
        ServiceUnbind unbindAnn = findAnnotation( method, ServiceUnbind.class );
        if( unbindAnn != null ) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if( parameterTypes.length == 0 ) {

                // @ServiceBind methods must have at least one parameter
                throw new IllegalStateException( "Method '" + method.getName() + "' in bean '" + beanDefinitionName + "' has no parameters, but has the @" + ServiceUnbind.class.getSimpleName() + " annotation" );

            } else {

                // add the new requirement
                requirements.add(
                        new ServiceUnbindRequirement(
                                this.tracker,
                                parameterTypes[ 0 ],
                                unbindAnn.filter(),
                                beanDefinitionName,
                                method ) );

            }
        }
    }

    private void detectMethodEndpoint( String beanDefinitionName,
                                       Method method,
                                       Collection<Requirement> requirements ) {

        Annotation metaAnnotation = findAnnotationWithMeta( method, MethodEndpointMarker.class );
        if( metaAnnotation != null ) {
            requirements.add(
                    new MethodEndpointRequirement( this.tracker, beanDefinitionName, method, metaAnnotation ) );
        }
    }

    private static <T extends Annotation> T findAnnotation( Method method, Class<T> annotationType ) {
        if( method.isAnnotationPresent( annotationType ) ) {
            return method.getAnnotation( annotationType );
        } else {
            for( Annotation annotation : method.getAnnotations() ) {
                T foundAnnotation = findAnnotation( annotation, annotationType );
                if( foundAnnotation != null ) {
                    return foundAnnotation;
                }
            }
            return null;
        }
    }

    private static <T extends Annotation> T findAnnotation( Annotation annotation, Class<T> annotationType ) {
        if( annotation.annotationType().getPackage().getName().startsWith( "java.lang" ) ) {
            return null;

        } else if( annotation.annotationType().isAnnotationPresent( annotationType ) ) {
            return annotation.annotationType().getAnnotation( annotationType );

        } else {
            for( Annotation nested : annotation.annotationType().getAnnotations() ) {
                T foundAnnotation = findAnnotation( nested, annotationType );
                if( foundAnnotation != null ) {
                    return foundAnnotation;
                }
            }
            return null;
        }
    }

    private static <T extends Annotation> Annotation findAnnotationWithMeta( Method method,
                                                                             Class<T> metaAnnotationType ) {
        if( method.isAnnotationPresent( metaAnnotationType ) ) {
            return method.getAnnotation( metaAnnotationType );
        } else {
            for( Annotation annotation : method.getAnnotations() ) {
                Annotation foundAnnotation = findAnnotationWithMeta( annotation, metaAnnotationType );
                if( foundAnnotation != null ) {
                    return foundAnnotation;
                }
            }
        }
        return null;
    }

    private static <T extends Annotation> Annotation findAnnotationWithMeta( Annotation annotation,
                                                                             Class<T> metaAnnotationType ) {
        if( annotation.annotationType().getPackage().getName().startsWith( "java.lang" ) ) {
            return null;

        } else if( annotation.annotationType().isAnnotationPresent( metaAnnotationType ) ) {
            return annotation;

        } else {
            for( Annotation nested : annotation.annotationType().getAnnotations() ) {
                Annotation foundAnnotation = findAnnotationWithMeta( nested, metaAnnotationType );
                if( foundAnnotation != null ) {
                    return foundAnnotation;
                }
            }
        }
        return null;
    }

    private static class RequirementPriorityComparator implements Comparator<Requirement> {

        @Override
        public int compare( Requirement o1, Requirement o2 ) {
            return new Integer( o1.getPriority() ).compareTo( o2.getPriority() );
        }
    }
}
