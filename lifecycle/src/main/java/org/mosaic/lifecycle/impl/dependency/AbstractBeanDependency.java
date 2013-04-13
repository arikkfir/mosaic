package org.mosaic.lifecycle.impl.dependency;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.lifecycle.annotation.ServiceProperty;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.reflection.MethodHandle;
import org.mosaic.util.reflection.MethodParameter;
import org.osgi.framework.ServiceReference;

/**
 * @author arik
 */
public abstract class AbstractBeanDependency extends AbstractDependency
{
    protected static class ServicePropertyParameterResolver implements MethodHandle.ParameterResolver
    {
        @Nullable
        @Override
        public Object resolve( @Nonnull MethodParameter parameter, @Nonnull MapEx<String, Object> resolveContext )
        {
            final ServiceProperty annotation = parameter.getAnnotation( ServiceProperty.class );
            if( annotation != null )
            {
                ServiceReference<?> serviceReference = resolveContext.get( "serviceReference", ServiceReference.class );
                if( serviceReference != null )
                {
                    String propertyName = annotation.value();
                    if( propertyName.trim().length() == 0 )
                    {
                        propertyName = parameter.getName();
                    }
                    return serviceReference.getProperty( propertyName );
                }
                else
                {
                    throw new IllegalStateException( "ServiceReference object not found in method handle invoker context" );
                }
            }
            return SKIP;
        }
    }

    protected static class ServiceInstanceResolver implements MethodHandle.ParameterResolver
    {
        @Nullable
        @Override
        public Object resolve( @Nonnull MethodParameter parameter, @Nonnull MapEx<String, Object> resolveContext )
        {
            return resolveContext.get( "service" );
        }
    }

    @Nonnull
    public abstract String getBeanName();

    public abstract void beanCreated( @Nonnull Object bean );

    public abstract void beanInitialized( @Nonnull Object bean );
}
