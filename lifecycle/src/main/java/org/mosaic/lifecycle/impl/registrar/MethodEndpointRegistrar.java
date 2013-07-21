package org.mosaic.lifecycle.impl.registrar;

import com.google.common.reflect.TypeToken;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.lifecycle.DP;
import org.mosaic.lifecycle.MethodEndpoint;
import org.mosaic.lifecycle.Module;
import org.mosaic.lifecycle.impl.ModuleImpl;
import org.mosaic.util.reflection.MethodHandle;
import org.mosaic.util.reflection.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;

/**
 * @author arik
 */
public class MethodEndpointRegistrar extends AbstractRegistrar implements MethodEndpoint
{
    @Nonnull
    private final Annotation annotation;

    @Nonnull
    private final MethodHandle methodHandle;

    public MethodEndpointRegistrar( @Nonnull ModuleImpl module,
                                    @Nonnull String beanName,
                                    @Nonnull Annotation annotation,
                                    @Nonnull MethodHandle methodHandle )
    {
        super( module, beanName );
        this.annotation = annotation;
        this.methodHandle = methodHandle;
    }

    @Nonnull
    @Override
    public Module getModule()
    {
        return this.module;
    }

    @Nonnull
    @Override
    public Annotation getType()
    {
        return this.annotation;
    }

    @Nonnull
    @Override
    public String getName()
    {
        return this.methodHandle.getName();
    }

    @Nonnull
    @Override
    public TypeToken<?> getReturnType()
    {
        return this.methodHandle.getReturnType();
    }

    @Nonnull
    @Override
    public Class<?> getDeclaringType()
    {
        return this.methodHandle.getDeclaringClass();
    }

    @Nonnull
    @Override
    public List<MethodParameter> getParameters()
    {
        return this.methodHandle.getParameters();
    }

    @Nonnull
    @Override
    public Collection<Annotation> getAnnotations()
    {
        return this.methodHandle.getAnnotations();
    }

    @Nullable
    @Override
    public <T extends Annotation> T getAnnotation( @Nonnull Class<T> annotationType )
    {
        return this.methodHandle.getAnnotation( annotationType );
    }

    @Nullable
    @Override
    public <T extends Annotation> T getAnnotation( @Nonnull Class<T> annotationType,
                                                   boolean checkOnClass,
                                                   boolean checkOnPackage )
    {
        T methodAnn = getAnnotation( annotationType );
        if( methodAnn != null )
        {
            return methodAnn;
        }

        Class<?> declaringClass = this.methodHandle.getDeclaringClass();
        if( checkOnClass )
        {
            T classAnn = declaringClass.getAnnotation( annotationType );
            if( classAnn != null )
            {
                return classAnn;
            }
        }

        if( checkOnPackage )
        {
            T classAnn = declaringClass.getAnnotation( annotationType );
            if( classAnn != null )
            {
                return classAnn;
            }
        }

        return null;
    }

    @Nonnull
    @Override
    public <T extends Annotation> T requireAnnotation( @Nonnull Class<T> annotationType )
    {
        return this.methodHandle.requireAnnotation( annotationType );
    }

    @Nonnull
    @Override
    public <T extends Annotation> T requireAnnotation( @Nonnull Class<T> annotationType,
                                                       boolean checkOnClass,
                                                       boolean checkOnPackage )
    {
        T ann = getAnnotation( annotationType, checkOnClass, checkOnPackage );
        if( ann == null )
        {
            throw new IllegalStateException( "Annotation '" + annotationType.getName() + "' is required but not found on endpoint '" + this + "'" );
        }
        else
        {
            return ann;
        }
    }

    @Nonnull
    @Override
    public Invoker createInvoker( @Nonnull MethodHandle.ParameterResolver... resolvers )
    {
        return new InvokerImpl( this.methodHandle.createInvoker( resolvers ) );
    }

    @Nonnull
    @Override
    public Invoker createInvoker( @Nonnull Collection<MethodHandle.ParameterResolver> resolvers )
    {
        return new InvokerImpl( this.methodHandle.createInvoker( resolvers ) );
    }

    @Override
    public String toString()
    {
        return "MethodEndpoint[" + this.methodHandle + " in bean '" + this.beanName + "']";
    }

    @Nonnull
    @Override
    protected Class<?> getServiceType()
    {
        return MethodEndpoint.class;
    }

    @Nullable
    @Override
    protected Object getServiceInstance()
    {
        return this;
    }

    @Nonnull
    @Override
    protected DP[] getServiceProperties()
    {
        Collection<DP> properties = new LinkedList<>();
        properties.add( DP.dp( "type", this.annotation.annotationType().getName() ) );

        Map<String, Object> attributes = AnnotationUtils.getAnnotationAttributes( this.annotation );
        for( Map.Entry<String, Object> entry : attributes.entrySet() )
        {
            properties.add( DP.dp( entry.getKey(), entry.getValue() ) );
        }
        return properties.toArray( new DP[ properties.size() ] );
    }

    private class InvokerImpl implements Invoker
    {
        @Nonnull
        private final MethodHandle.Invoker invoker;

        private InvokerImpl( @Nonnull MethodHandle.Invoker invoker )
        {
            this.invoker = invoker;
        }

        @Nonnull
        @Override
        public MethodEndpoint getMethodEndpoint()
        {
            return MethodEndpointRegistrar.this;
        }

        @Nonnull
        @Override
        public Invocation resolve( @Nonnull Map<String, Object> resolveContext )
        {
            Object bean = module.getBean( beanName );
            if( bean != null )
            {
                return new InvocationImpl( this.invoker.resolve( resolveContext ), bean );
            }
            else
            {
                throw new IllegalStateException( MethodEndpointRegistrar.this.toString() + " is no longer available" );
            }
        }

        private class InvocationImpl implements Invocation
        {
            @Nonnull
            private final MethodHandle.Invocation invocation;

            @Nonnull
            private final Object bean;

            private InvocationImpl( @Nonnull MethodHandle.Invocation invocation, @Nonnull Object bean )
            {
                this.invocation = invocation;
                this.bean = bean;
            }

            @Nonnull
            @Override
            public Invoker getInvoker()
            {
                return InvokerImpl.this;
            }

            @Nonnull
            @Override
            public Object[] getArguments()
            {
                return this.invocation.getArguments();
            }

            @Nullable
            @Override
            public Object invoke() throws Exception
            {
                return this.invocation.invoke( this.bean );
            }
        }
    }
}
