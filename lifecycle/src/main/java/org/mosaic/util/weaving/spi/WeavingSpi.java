package org.mosaic.util.weaving.spi;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.reflection.MethodHandle;
import org.mosaic.util.reflection.impl.MethodHandleFactoryImpl;
import org.mosaic.util.weaving.MethodInterceptor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
public final class WeavingSpi implements ServiceTrackerCustomizer<MethodInterceptor, MethodInterceptor>
{
    private static final Logger LOG = LoggerFactory.getLogger( WeavingSpi.class );

    private static final WeavingSpi instance = new WeavingSpi();

    public static WeavingSpi getInstance()
    {
        return WeavingSpi.instance;
    }

    @Nonnull
    private final BundleContext bundleContext;

    @Nullable
    private MethodHandleFactoryImpl methodHandleFactory;

    @Nullable
    private ServiceTracker<MethodInterceptor, MethodInterceptor> methodInterceptorsTracker;

    @Nonnull
    private List<MethodInterceptor> methodInterceptors = Collections.emptyList();

    private WeavingSpi()
    {
        this.bundleContext = FrameworkUtil.getBundle( getClass() ).getBundleContext();
    }

    public void setMethodHandleFactory( @Nullable MethodHandleFactoryImpl methodHandleFactory )
    {
        this.methodHandleFactory = methodHandleFactory;
    }

    @Override
    public MethodInterceptor addingService( ServiceReference<MethodInterceptor> reference )
    {
        MethodInterceptor interceptor = WeavingSpi.this.bundleContext.getService( reference );
        if( interceptor != null )
        {
            List<MethodInterceptor> interceptors = new LinkedList<>( WeavingSpi.this.methodInterceptors );
            interceptors.add( interceptor );
            // TODO arik: sort by rank
            WeavingSpi.this.methodInterceptors = interceptors;
            LOG.debug( "Added method interceptor {}", interceptor );
        }
        return interceptor;
    }

    @Override
    public void modifiedService( ServiceReference<MethodInterceptor> reference, MethodInterceptor service )
    {
        // no-op
    }

    @Override
    public void removedService( ServiceReference<MethodInterceptor> reference, MethodInterceptor interceptor )
    {
        List<MethodInterceptor> interceptors = new LinkedList<>( WeavingSpi.this.methodInterceptors );
        interceptors.remove( interceptor );
        WeavingSpi.this.methodInterceptors = interceptors;
        LOG.debug( "Removed method interceptor {}", interceptor );
    }

    @SuppressWarnings("UnusedDeclaration")
    @Nullable
    public Object intercept( @Nonnull Object object,
                             @Nonnull Class<?> type,
                             @Nonnull String methodName,
                             @Nonnull Class<?>[] parameterTypes,
                             @Nonnull Object[] parameterValues ) throws Exception
    {
        return new MethodInvocationImpl( object, type, methodName, parameterTypes, parameterValues ).proceed();
    }

    @Nonnull
    private List<MethodInterceptor> getMethodInterceptors()
    {
        if( this.methodInterceptorsTracker == null )
        {
            synchronized( this )
            {
                if( this.methodInterceptorsTracker == null )
                {
                    this.methodInterceptorsTracker = new ServiceTracker<>( this.bundleContext, MethodInterceptor.class, this );
                    this.methodInterceptorsTracker.open();
                }
            }
        }
        return this.methodInterceptors;
    }

    private class MethodInvocationImpl implements MethodInterceptor.MethodInvocation
    {
        @Nonnull
        private final Object object;

        @Nonnull
        private final MethodHandle methodHandle;

        @Nonnull
        private final MethodHandle realMethodHandle;

        @Nonnull
        private final List<MethodInterceptor> interceptors;

        @Nonnull
        private Object[] arguments;

        private int nextInterceptorIndex = 0;

        private MethodInvocationImpl( @Nonnull Object object,
                                      @Nonnull Class<?> type,
                                      @Nonnull String methodName,
                                      @Nonnull Class<?>[] parameterTypes,
                                      @Nonnull Object[] parameterValues )
        {
            this.object = object;
            this.arguments = parameterValues;

            MethodHandleFactoryImpl methodHandleFactory = WeavingSpi.this.methodHandleFactory;
            if( methodHandleFactory == null )
            {
                throw new IllegalStateException( "MethodHandleFactory has not been set" );
            }

            MethodHandle methodHandle = methodHandleFactory.findMethodHandle( type, methodName, parameterTypes );
            if( methodHandle == null )
            {
                throw new IllegalStateException( "Could not find method '" + methodName + "' with parameters " + Arrays.asList( parameterTypes ) + " in class " + type.getName() );
            }
            this.methodHandle = methodHandle;

            MethodHandle realMethodHandle = methodHandleFactory.findMethodHandle( type, methodName + "$$Impl", parameterTypes );
            if( realMethodHandle == null )
            {
                throw new IllegalStateException( "Could not find method '" + methodName + "$$Impl" + "' with parameters " + Arrays.asList( parameterTypes ) + " in class " + type.getName() );
            }
            this.realMethodHandle = realMethodHandle;

            this.interceptors = getMethodInterceptors();
        }

        @Nonnull
        @Override
        public MethodHandle getMethodHandle()
        {
            return this.methodHandle;
        }

        @Nonnull
        @Override
        public Object getObject()
        {
            return this.object;
        }

        @Nonnull
        @Override
        public Object[] getArguments()
        {
            return this.arguments;
        }

        @Nullable
        @Override
        public Object proceed() throws Exception
        {
            return proceed( this.arguments );
        }

        @Nullable
        @Override
        public Object proceed( @Nonnull Object[] arguments ) throws Exception
        {
            this.arguments = arguments;
            if( this.nextInterceptorIndex < this.interceptors.size() )
            {
                return this.interceptors.get( this.nextInterceptorIndex++ ).intercept( this );
            }
            else
            {
                return this.realMethodHandle.getNativeMethod().invoke( this.object, this.arguments );
            }
        }
    }
}
