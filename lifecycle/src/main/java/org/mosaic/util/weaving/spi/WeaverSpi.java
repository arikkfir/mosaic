package org.mosaic.util.weaving.spi;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.reflection.MethodHandle;
import org.mosaic.util.reflection.MethodHandleFactory;
import org.mosaic.util.weaving.MethodInterceptor;
import org.mosaic.util.weaving.MethodInvocation;
import org.mosaic.util.weaving.impl.InterceptionWeavingHook;

/**
 * @author arik
 */
public class WeaverSpi
{
    private static final Object[] EMPTY_ARGS_ARRAY = new Object[ 0 ];

    private static WeaverSpi instance;

    @SuppressWarnings("UnusedDeclaration")
    public static MethodInvocation createInvocation( @Nonnull Class<?> targetType,
                                                     @Nonnull String methodName,
                                                     @Nonnull Class<?>[] parameterTypes,
                                                     @Nullable Object target )
    {
        WeaverSpi instance = WeaverSpi.instance;
        if( instance != null )
        {
            return instance.createInvocationImpl( targetType, methodName, parameterTypes, target );
        }
        else
        {
            throw new IllegalStateException( "Weaving framework is not active" );
        }
    }

    @Nonnull
    private final InterceptionWeavingHook interceptionWeavingHook;

    @Nonnull
    private final MethodHandleFactory methodHandleFactory;

    public WeaverSpi( @Nonnull InterceptionWeavingHook hook, @Nonnull MethodHandleFactory methodHandleFactory )
    {
        this.methodHandleFactory = methodHandleFactory;
        this.interceptionWeavingHook = hook;
        WeaverSpi.instance = this;
    }

    private MethodInvocation createInvocationImpl( @Nonnull Class<?> targetType,
                                                   @Nonnull String methodName,
                                                   @Nonnull Class<?>[] parameterTypes,
                                                   @Nullable Object target )
    {
        return new MethodInvocationImpl( this.interceptionWeavingHook.getInterceptors(),
                                         targetType,
                                         methodName,
                                         parameterTypes,
                                         target );
    }

    private class MethodInvocationImpl implements MethodInvocation
    {
        @Nonnull
        private final Iterator<MethodInterceptor> interceptors;

        @Nonnull
        private final Class<?> targetType;

        @Nonnull
        private final String methodName;

        @Nonnull
        private final Class<?>[] parameterTypes;

        @Nullable
        private final Object target;

        @Nonnull
        private final Object[] arguments;

        @Nullable
        private MethodHandle methodHandle;

        private MethodInvocationImpl( @Nonnull Collection<MethodInterceptor> interceptors,
                                      @Nonnull Class<?> targetType,
                                      @Nonnull String methodName,
                                      @Nonnull Class<?>[] parameterTypes,
                                      @Nullable Object target )
        {
            this.interceptors = interceptors.iterator();
            this.targetType = targetType;
            this.methodName = methodName;
            this.parameterTypes = parameterTypes;
            this.target = target;
            this.arguments = EMPTY_ARGS_ARRAY;
        }

        private MethodInvocationImpl( @Nonnull MethodInvocationImpl invocation, @Nonnull Object[] arguments )
        {
            this.interceptors = invocation.interceptors;
            this.targetType = invocation.targetType;
            this.methodName = invocation.methodName;
            this.parameterTypes = invocation.parameterTypes;
            this.target = invocation.target;
            this.arguments = arguments;
            this.methodHandle = invocation.methodHandle;
        }

        @Nonnull
        @Override
        public MethodHandle getMethodHandle()
        {
            if( this.methodHandle == null )
            {
                MethodHandle methodHandle = methodHandleFactory.findMethodHandle( this.targetType, this.methodName, this.parameterTypes );
                if( methodHandle == null )
                {
                    throw new IllegalStateException( "Could not find method '" + methodName + "' in class '" + targetType.getName() + "' with parameters " + Arrays.asList( parameterTypes ) );
                }
                else
                {
                    this.methodHandle = methodHandle;
                }
            }
            return this.methodHandle;
        }

        @Nullable
        @Override
        public Object getObject()
        {
            return this.target;
        }

        @Nonnull
        @Override
        public Object[] getArguments()
        {
            return this.arguments;
        }

        @Nullable
        @Override
        public Object proceed( @Nonnull Object[] arguments ) throws Exception
        {
            if( this.interceptors.hasNext() )
            {
                MethodInterceptor interceptor = this.interceptors.next();
                return interceptor.intercept( new MethodInvocationImpl( this, arguments ) );
            }
            else
            {
                return getMethodHandle().invoke( this.target, this.arguments );
            }
        }
    }
}
