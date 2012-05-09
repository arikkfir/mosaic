package org.mosaic.server.boot.impl.publish.requirement;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import org.mosaic.lifecycle.*;
import org.mosaic.server.boot.impl.publish.BundleTracker;
import org.mosaic.server.boot.impl.publish.spring.BundleBeanFactory;
import org.mosaic.server.boot.impl.publish.spring.OsgiSpringNamespacePlugin;
import org.mosaic.server.boot.impl.publish.spring.SpringUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

import static java.lang.String.format;
import static org.springframework.core.GenericCollectionTypeResolver.getCollectionParameterType;

/**
 * @author arik
 */
public class RequirementFactory
{
    private static interface ServiceRequirementFactory<T extends Annotation>
    {
        Requirement createRequirement( BundleTracker tracker,
                                       Class<?> serviceType,
                                       boolean list, T annotation,
                                       String beanDefinitionName, Method method );
    }

    private final BundleTracker tracker;

    private final Bundle bundle;

    private final OsgiSpringNamespacePlugin osgiSpringNamespacePlugin;

    public RequirementFactory( BundleTracker tracker,
                               Bundle bundle,
                               OsgiSpringNamespacePlugin osgiSpringNamespacePlugin )
    {
        this.tracker = tracker;
        this.bundle = bundle;
        this.osgiSpringNamespacePlugin = osgiSpringNamespacePlugin;
    }

    public Collection<Requirement> detectRequirements()
    {
        BundleBeanFactory beanFactory = new BundleBeanFactory( this.bundle, this.osgiSpringNamespacePlugin );

        List<Requirement> requirements = new LinkedList<>();

        // detect requirement in classes
        for( String beanDefinitionName : beanFactory.getBeanDefinitionNames() )
        {
            Class<?> beanClass = SpringUtils.getBeanClass( this.bundle, beanFactory, beanDefinitionName );
            if( beanClass != null )
            {
                // detect service classes for registration
                ServiceExport exportAnn = beanClass.getAnnotation( ServiceExport.class );
                if( exportAnn != null )
                {
                    int rank = 0;
                    Rank rankAnn = AnnotationUtils.findAnnotation( beanClass, Rank.class );
                    if( rankAnn != null )
                    {
                        rank = rankAnn.value();
                    }
                    requirements.add( new ServiceExportRequirement( this.tracker, beanDefinitionName, exportAnn.value(), rank ) );
                }

                // detect method requirements
                for( Method method : ReflectionUtils.getUniqueDeclaredMethods( beanClass ) )
                {
                    detectBundleContextRef( beanDefinitionName, method, requirements );
                    detectServiceReferences( beanDefinitionName, method, requirements );
                    detectServiceBindings( beanDefinitionName, method, requirements );
                    detectMethodEndpoint( beanDefinitionName, method, requirements );
                }
            }
        }

        // detect web capabilities and requirements
        if( this.bundle.getEntry( "/web" ) != null )
        {
            requirements.add( new WebModuleInfoRequirement( this.tracker ) );
        }

        // sort and return
        Collections.sort( requirements, new RequirementPriorityComparator() );
        return requirements;
    }

    private void detectBundleContextRef( String beanDefinitionName, Method method, List<Requirement> requirements )
    {
        ContextRef contextRefAnn = findAnnotation( method, ContextRef.class );
        if( contextRefAnn == null )
        {
            return;
        }

        Class<?>[] parameterTypes = method.getParameterTypes();
        if( parameterTypes.length == 0 )
        {
            throw new IllegalStateException( format( "Method '%s' in bean '%s' has the @%s annotation, but has no parameters",
                                                     method.getName(),
                                                     beanDefinitionName,
                                                     ContextRef.class.getSimpleName() ) );
        }
        else if( parameterTypes.length > 1 )
        {
            // @ContextRef methods must have exactly one parameter
            throw new IllegalStateException( format( "Method '%s' in bean '%s' has the @%s annotation, but has more than one parameter",
                                                     method.getName(),
                                                     beanDefinitionName,
                                                     ContextRef.class.getSimpleName() ) );
        }
        else if( !parameterTypes[ 0 ].isAssignableFrom( BundleContext.class ) )
        {
            // @ContextRef methods must have exactly one parameter
            throw new IllegalStateException( format( "Method '%s' in bean '%s' has the @%s annotation, but has a parameter of an illegal type (%s): must be of type '%s'",
                                                     method.getName(),
                                                     beanDefinitionName,
                                                     ContextRef.class.getSimpleName(),
                                                     parameterTypes[ 0 ].getSimpleName(),
                                                     BundleContext.class.getSimpleName() ) );
        }
        else
        {
            // add the new requirement
            requirements.add( new BundleContextRequirement( this.tracker, beanDefinitionName, method ) );
        }
    }

    private void detectServiceReferences( String beanDefinitionName,
                                          Method method,
                                          Collection<Requirement> requirements )
    {
        createRequirement( requirements, beanDefinitionName, method, ServiceRef.class, true, new ServiceRequirementFactory<ServiceRef>()
        {
            @Override
            public Requirement createRequirement( BundleTracker tracker,
                                                  Class<?> serviceType,
                                                  boolean list, ServiceRef annotation,
                                                  String beanDefinitionName,
                                                  Method method )
            {
                if( list )
                {
                    return new ServiceListRequirement( tracker, serviceType, annotation.filter(), beanDefinitionName, method );
                }
                else
                {
                    return new ServiceRefRequirement( tracker, serviceType, annotation.filter(), annotation.required(), beanDefinitionName, method );
                }
            }
        } );
    }

    private void detectServiceBindings( String beanDefinitionName, Method method, Collection<Requirement> requirements )
    {
        createRequirement( requirements, beanDefinitionName, method, ServiceBind.class, true, new ServiceRequirementFactory<ServiceBind>()
        {
            @Override
            public Requirement createRequirement( BundleTracker tracker,
                                                  Class<?> serviceType,
                                                  boolean list, ServiceBind annotation,
                                                  String beanDefinitionName,
                                                  Method method )
            {
                return new ServiceBindRequirement( tracker,
                                                   serviceType,
                                                   annotation.filter(),
                                                   beanDefinitionName,
                                                   method );
            }
        } );

        createRequirement( requirements, beanDefinitionName, method, ServiceUnbind.class, true, new ServiceRequirementFactory<ServiceUnbind>()
        {
            @Override
            public Requirement createRequirement( BundleTracker tracker,
                                                  Class<?> serviceType,
                                                  boolean list, ServiceUnbind annotation,
                                                  String beanDefinitionName,
                                                  Method method )
            {
                return new ServiceUnbindRequirement( tracker,
                                                     serviceType,
                                                     annotation.filter(),
                                                     beanDefinitionName,
                                                     method );
            }
        } );
    }

    private void detectMethodEndpoint( String beanDefinitionName, Method method, Collection<Requirement> requirements )
    {
        Annotation metaAnnotation = findAnnotationWithMeta( method, MethodEndpointMarker.class );
        if( metaAnnotation != null )
        {
            int rank = 0;
            Rank rankAnn = AnnotationUtils.findAnnotation( method, Rank.class );
            if( rankAnn != null )
            {
                rank = rankAnn.value();
            }
            requirements.add( new MethodEndpointRequirement( this.tracker, beanDefinitionName, method, metaAnnotation, rank ) );
        }
    }

    private <T extends Annotation> void createRequirement( Collection<Requirement> requirements,
                                                           String beanDefinitionName,
                                                           Method method,
                                                           Class<T> annotationType,
                                                           boolean supportLists,
                                                           ServiceRequirementFactory<T> factory )
    {
        // no annotation? no soup!
        T annotation = findAnnotation( method, annotationType );
        if( annotation == null )
        {
            return;
        }

        // no parameters? no soup!
        Class<?>[] parameterTypes = method.getParameterTypes();
        if( parameterTypes.length == 0 )
        {
            throw new IllegalStateException( format( "Method '%s' in bean '%s' has the @%s annotation, but has no parameters",
                                                     method.getName(),
                                                     beanDefinitionName,
                                                     annotationType.getSimpleName() ) );
        }

        // iterate over parameters, and attempt to discover the type of service this requirement refers to
        Class<?> serviceType = null;
        boolean list = false;
        for( int i = 0, parameterTypesLength = parameterTypes.length; i < parameterTypesLength; i++ )
        {
            Class<?> parameterType = parameterTypes[ i ];
            MethodParameter methodParameter = new MethodParameter( method, i );

            if( parameterType.isAssignableFrom( ServiceReference.class ) )
            {
                // extract service type from generic ServiceReference<SERVICE_TYPE_HERE> declaration
                // ignores cases where the service reference points to a wildcard (e.g. ServiceReference<?...>)
                Type genericParameter = methodParameter.getGenericParameterType();
                if( !( genericParameter instanceof ParameterizedType ) )
                {
                    continue;
                }

                ParameterizedType parameterizedType = ( ParameterizedType ) genericParameter;
                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                if( actualTypeArguments.length != 1 || !( actualTypeArguments[ 0 ] instanceof Class<?> ) )
                {
                    continue;
                }

                Class<?> serviceRefParameter = ( Class<?> ) actualTypeArguments[ 0 ];
                if( serviceType == null )
                {
                    serviceType = serviceRefParameter;
                }
                else if( !serviceType.equals( serviceRefParameter ) )
                {
                    throw new IllegalStateException( format( "Conflicting service types in method '%s' of bean '%s'",
                                                             method.getName(), beanDefinitionName ) );
                }
            }
            else if( supportLists && parameterType.isAssignableFrom( List.class ) )
            {
                // a requirement on a dynamic list of services for a given type
                Class<?> itemType = getCollectionParameterType( methodParameter );
                if( itemType == null )
                {
                    throw new IllegalStateException( format( "Untyped 'List' parameter in method '%s' of bean '%s'",
                                                             method.getName(), beanDefinitionName ) );
                }
                else if( serviceType == null )
                {
                    serviceType = itemType;
                    list = true;
                }
                else if( !serviceType.equals( itemType ) )
                {
                    throw new IllegalStateException( format( "Conflicting service types in method '%s' of bean '%s'",
                                                             method.getName(), beanDefinitionName ) );
                }
            }
            else if( serviceType == null )
            {
                serviceType = parameterType;
            }
            else if( !serviceType.equals( parameterType ) )
            {
                throw new IllegalStateException( format( "Conflicting service types in method '%s' of bean '%s'",
                                                         method.getName(), beanDefinitionName ) );
            }
        }

        // if no service type could be inferred, fail; otherwise, create and add the new requirement using given factory
        if( serviceType == null )
        {
            throw new IllegalStateException( format( "Could not infer service type from signature of @%s method '%s' in bean '%s' - check your parameters",
                                                     annotationType.getSimpleName(),
                                                     method.getName(),
                                                     beanDefinitionName ) );
        }
        else
        {
            requirements.add( factory.createRequirement( this.tracker, serviceType, list, annotation, beanDefinitionName, method ) );
        }
    }

    private static <T extends Annotation> T findAnnotation( Method method, Class<T> annotationType )
    {
        if( method.isAnnotationPresent( annotationType ) )
        {
            return method.getAnnotation( annotationType );
        }
        else
        {
            for( Annotation annotation : method.getAnnotations() )
            {
                T foundAnnotation = findAnnotation( annotation, annotationType );
                if( foundAnnotation != null )
                {
                    return foundAnnotation;
                }
            }
            return null;
        }
    }

    private static <T extends Annotation> T findAnnotation( Annotation annotation, Class<T> annotationType )
    {
        if( annotation.annotationType().getPackage().getName().startsWith( "java.lang" ) )
        {
            return null;

        }
        else if( annotation.annotationType().isAnnotationPresent( annotationType ) )
        {
            return annotation.annotationType().getAnnotation( annotationType );

        }
        else
        {
            for( Annotation nested : annotation.annotationType().getAnnotations() )
            {
                T foundAnnotation = findAnnotation( nested, annotationType );
                if( foundAnnotation != null )
                {
                    return foundAnnotation;
                }
            }
            return null;
        }
    }

    private static <T extends Annotation> Annotation findAnnotationWithMeta( Method method,
                                                                             Class<T> metaAnnotationType )
    {
        if( method.isAnnotationPresent( metaAnnotationType ) )
        {
            return method.getAnnotation( metaAnnotationType );
        }
        else
        {
            for( Annotation annotation : method.getAnnotations() )
            {
                Annotation foundAnnotation = findAnnotationWithMeta( annotation, metaAnnotationType );
                if( foundAnnotation != null )
                {
                    return foundAnnotation;
                }
            }
        }
        return null;
    }

    private static <T extends Annotation> Annotation findAnnotationWithMeta( Annotation annotation,
                                                                             Class<T> metaAnnotationType )
    {
        if( annotation.annotationType().getPackage().getName().startsWith( "java.lang" ) )
        {
            return null;

        }
        else if( annotation.annotationType().isAnnotationPresent( metaAnnotationType ) )
        {
            return annotation;

        }
        else
        {
            for( Annotation nested : annotation.annotationType().getAnnotations() )
            {
                Annotation foundAnnotation = findAnnotationWithMeta( nested, metaAnnotationType );
                if( foundAnnotation != null )
                {
                    return foundAnnotation;
                }
            }
        }
        return null;
    }

    private static class RequirementPriorityComparator implements Comparator<Requirement>
    {
        @Override
        public int compare( Requirement o1, Requirement o2 )
        {
            return new Integer( o1.getPriority() ).compareTo( o2.getPriority() );
        }
    }
}
