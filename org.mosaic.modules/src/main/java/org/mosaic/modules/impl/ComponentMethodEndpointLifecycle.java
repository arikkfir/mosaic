package org.mosaic.modules.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.modules.*;
import org.mosaic.util.collections.EmptyMapEx;
import org.mosaic.util.collections.HashMapEx;
import org.mosaic.util.collections.MapEx;
import org.mosaic.util.reflection.InvokableMethodHandle;
import org.mosaic.util.reflection.MethodHandle;
import org.mosaic.util.reflection.MethodHandleFactory;
import org.mosaic.util.reflection.ParameterResolver;
import org.osgi.framework.*;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * @author arik
 */
final class ComponentMethodEndpointLifecycle extends Lifecycle implements MethodEndpoint, ServiceCapabilityProvider
{
    @Nonnull
    private final ComponentDescriptorImpl<?> componentDescriptor;

    @Nonnull
    private final Method method;

    @Nonnull
    private final Annotation annotation;

    @Nonnull
    private final Dictionary<String, Object> properties;

    @Nullable
    private ServiceRegistration<MethodEndpoint> registration;

    ComponentMethodEndpointLifecycle( @Nonnull ComponentDescriptorImpl<?> componentDescriptor,
                                      @Nonnull Method method,
                                      @Nonnull Annotation annotation )
    {
        this.componentDescriptor = componentDescriptor;
        this.method = method;
        this.method.setAccessible( true );

        this.annotation = annotation;
        this.properties = new Hashtable<>();
        this.properties.put( "name", getName() );
        this.properties.put( "type", this.annotation.annotationType().getName() );
        for( Method endpointTypeMethod : this.annotation.annotationType().getDeclaredMethods() )
        {
            String name = endpointTypeMethod.getName();
            try
            {
                this.properties.put( name, endpointTypeMethod.invoke( this.annotation ) );
            }
            catch( Throwable e )
            {
                String msg = "error adding attribute '" + name + "' from annotation '" + this.annotation.annotationType().getName() + "'";
                throw new ComponentDefinitionException( msg, e, this.componentDescriptor.getComponentType(), this.componentDescriptor.getModule() );
            }
        }

        Ranking rankingAnn = this.method.getAnnotation( Ranking.class );
        this.properties.put( Constants.SERVICE_RANKING, rankingAnn == null ? 0 : rankingAnn.value() );
    }

    @Override
    public final String toString()
    {
        return "ComponentMethodEndpoint[" + this.method.getName() + " in " + this.componentDescriptor + "]";
    }

    @Nonnull
    @Override
    public List<ModuleWiring.ServiceCapability> getServiceCapabilities()
    {
        ServiceRegistration<MethodEndpoint> registration = this.registration;
        return registration == null
               ? Collections.<ModuleWiring.ServiceCapability>emptyList()
               : Arrays.<ModuleWiring.ServiceCapability>asList( new ServiceCapabilityImpl( registration ) );
    }

    @Nonnull
    @Override
    public String getName()
    {
        return this.method.getName();
    }

    @Nonnull
    @Override
    public Annotation getType()
    {
        return this.annotation;
    }

    @Nonnull
    @Override
    public InvokableMethodHandle getMethodHandle()
    {
        BundleContext bundleContext = getMyBundleContext();

        org.osgi.framework.ServiceReference<MethodHandleFactory> sr = bundleContext.getServiceReference( MethodHandleFactory.class );
        if( sr == null )
        {
            throw new IllegalStateException( "could not find MethodHandleFactory service" );
        }

        MethodHandleFactory methodHandleFactory = bundleContext.getService( sr );
        try
        {
            return methodHandleFactory.findMethodHandle( this.method );
        }
        finally
        {
            bundleContext.ungetService( sr );
        }
    }

    @Nonnull
    @Override
    public Invoker createInvoker( @Nonnull ParameterResolver... resolvers )
    {
        return new MethodEndpointInvokerImpl( resolvers );
    }

    @Nullable
    @Override
    public Object invoke( @Nullable Object... args ) throws Throwable
    {
        try
        {
            return this.method.invoke( this.componentDescriptor.getInstance(), args );
        }
        catch( InvocationTargetException e )
        {
            throw e.getCause();
        }
    }

    @Override
    protected synchronized void onAfterActivate()
    {
        BundleContext bundleContext = this.componentDescriptor.getModule().getBundle().getBundleContext();
        if( bundleContext != null )
        {
            this.registration = bundleContext.registerService( MethodEndpoint.class, this, this.properties );
        }
    }

    @Override
    protected synchronized void onBeforeDeactivate()
    {
        if( this.registration != null )
        {
            try
            {
                this.registration.unregister();
            }
            catch( Exception ignore )
            {
            }
            this.registration = null;
        }
    }

    private class MethodEndpointInvokerImpl implements Invoker
    {
        @Nonnull
        private final InvokableMethodHandle.Invoker targetInvoker;

        private MethodEndpointInvokerImpl( @Nonnull ParameterResolver... resolvers )
        {
            this.targetInvoker = getMethodHandle().createInvoker( resolvers );
        }

        @Nonnull
        @Override
        public MethodHandle getMethod()
        {
            return this.targetInvoker.getMethod();
        }

        @Nonnull
        @Override
        public Invocation resolve( @Nonnull Map<String, Object> resolveContext )
        {
            return new MethodEndpointInvokerImpl.MethodEndpointInvocationImpl( resolveContext );
        }

        private class MethodEndpointInvocationImpl implements Invocation
        {
            @Nonnull
            private final InvokableMethodHandle.Invocation targetInvocation;

            private MethodEndpointInvocationImpl( @Nonnull Map<String, Object> resolveContext )
            {
                this.targetInvocation = MethodEndpointInvokerImpl.this.targetInvoker.resolve( resolveContext );
            }

            @Nonnull
            @Override
            public Invoker getInvoker()
            {
                return MethodEndpointInvokerImpl.this;
            }

            @Nonnull
            @Override
            public Object[] getArguments()
            {
                return this.targetInvocation.getArguments();
            }

            @Nullable
            @Override
            public Object invoke() throws Exception
            {
                return this.targetInvocation.invoke( ComponentMethodEndpointLifecycle.this.componentDescriptor.getInstance() );
            }
        }
    }

    private class ServiceCapabilityImpl implements ModuleWiring.ServiceCapability
    {
        @Nonnull
        private final ServiceRegistration registration;

        private ServiceCapabilityImpl( @Nonnull ServiceRegistration registration )
        {
            this.registration = registration;
        }

        @Override
        public long getId()
        {
            ServiceReference reference = this.registration.getReference();
            if( reference != null )
            {
                Long id = ( Long ) reference.getProperty( Constants.SERVICE_ID );
                if( id != null )
                {
                    return id;
                }
            }
            throw new IllegalStateException( "id not found" );
        }

        @Nonnull
        @Override
        public Module getProvider()
        {
            return ComponentMethodEndpointLifecycle.this.componentDescriptor.getModule();
        }

        @Nonnull
        @Override
        public Class<?> getType()
        {
            return MethodEndpoint.class;
        }

        @Nonnull
        @Override
        public MapEx<String, Object> getProperties()
        {
            ServiceReference reference = this.registration.getReference();
            if( reference != null )
            {
                MapEx<String, Object> properties = new HashMapEx<>();
                for( String key : reference.getPropertyKeys() )
                {
                    properties.put( key, reference.getProperty( key ) );
                }
                return properties;
            }
            return EmptyMapEx.emptyMapEx();
        }

        @Nonnull
        @Override
        public Collection<Module> getConsumers()
        {
            ServiceReference<?> reference = this.registration.getReference();
            if( reference != null )
            {
                Bundle[] usingBundles = reference.getUsingBundles();
                if( usingBundles != null )
                {
                    List<Module> consumers = new LinkedList<>();
                    for( Bundle bundle : usingBundles )
                    {
                        consumers.add( Activator.getModuleManager().getModule( bundle.getBundleId() ) );
                    }
                    return consumers;
                }
            }
            return Collections.emptyList();
        }
    }
}
