package org.mosaic.util.weaving.spi;

import com.google.common.collect.ComparisonChain;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
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

import static org.mosaic.lifecycle.impl.util.ServiceUtils.getId;
import static org.mosaic.lifecycle.impl.util.ServiceUtils.getRanking;

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
    private List<MethodInterceptorAdapter> methodInterceptors = Collections.emptyList();

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
            List<MethodInterceptorAdapter> interceptors = new LinkedList<>( WeavingSpi.this.methodInterceptors );
            interceptors.add( new MethodInterceptorAdapter( getId( reference ), getRanking( reference ), interceptor ) );
            Collections.sort( interceptors );
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
        List<MethodInterceptorAdapter> interceptors = new LinkedList<>( WeavingSpi.this.methodInterceptors );
        for( Iterator<MethodInterceptorAdapter> iterator = interceptors.iterator(); iterator.hasNext(); )
        {
            MethodInterceptorAdapter adapter = iterator.next();
            if( adapter.id == getId( reference ) )
            {
                iterator.remove();
                break;
            }
        }
        WeavingSpi.this.methodInterceptors = interceptors;
        LOG.debug( "Removed method interceptor {}", interceptor );
    }

    @SuppressWarnings("UnusedDeclaration")
    @Nullable
    public Object intercept( @Nonnull Object object,
                             @Nonnull Class<?> type,
                             @Nonnull String methodName,
                             @Nonnull Class<?>[] parameterTypes,
                             @Nonnull Object[] parameterValues ) throws Throwable
    {
        return new MethodInvocationImpl( object, type, methodName, parameterTypes, parameterValues ).proceed();
    }

    @Nonnull
    private List<? extends MethodInterceptor> getMethodInterceptors()
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

    private class MethodInterceptorAdapter implements MethodInterceptor, Comparable<MethodInterceptorAdapter>
    {
        private final long id;

        private final int rank;

        @Nonnull
        private final MethodInterceptor target;

        private MethodInterceptorAdapter( long id, int rank, @Nonnull MethodInterceptor target )
        {
            this.id = id;
            this.target = target;
            this.rank = rank;
        }

        @Override
        public int compareTo( @Nonnull MethodInterceptorAdapter o )
        {
            return ComparisonChain.start()
                                  .compare( o.rank, this.rank )
                                  .compare( this.id, o.id )
                                  .result();
        }

        @Nullable
        @Override
        public Object intercept( @Nonnull MethodInvocation invocation ) throws Throwable
        {
            return this.target.intercept( invocation );
        }
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
        private final List<? extends MethodInterceptor> interceptors;

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
        public Object proceed() throws Throwable
        {
            return proceed( this.arguments );
        }

        @Nullable
        @Override
        public Object proceed( @Nonnull Object[] arguments ) throws Throwable
        {
            this.arguments = arguments;
            if( this.nextInterceptorIndex < this.interceptors.size() )
            {
                return this.interceptors.get( this.nextInterceptorIndex++ ).intercept( this );
            }
            else
            {
                try
                {
                    return this.realMethodHandle.getNativeMethod().invoke( this.object, this.arguments );
                }
                catch( InvocationTargetException e )
                {
                    throw e.getCause();
                }
            }
        }
    }
}
