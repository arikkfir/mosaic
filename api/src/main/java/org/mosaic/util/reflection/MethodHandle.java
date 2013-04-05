package org.mosaic.util.reflection;

import com.google.common.reflect.TypeToken;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public interface MethodHandle
{
    class UnresolvableArgumentException extends RuntimeException
    {
        @Nonnull
        private final MethodParameter parameter;

        public UnresolvableArgumentException( @Nonnull MethodParameter parameter )
        {
            this( "no parameter resolver provided a value for this parameter", parameter );
        }

        public UnresolvableArgumentException( @Nonnull String message, @Nonnull MethodParameter parameter )
        {
            super( "Could not resolve value for parameter '" + parameter + "': " + message );
            this.parameter = parameter;
        }

        public UnresolvableArgumentException( Throwable cause, @Nonnull MethodParameter parameter )
        {
            this( cause.getMessage(), cause, parameter );
        }

        public UnresolvableArgumentException( @Nonnull String message,
                                              @Nonnull Throwable cause,
                                              @Nonnull MethodParameter parameter )
        {
            super( "Could not resolve value for parameter '" + parameter + "': " + message, cause );
            this.parameter = parameter;
        }

        @Nonnull
        public MethodParameter getParameter()
        {
            return parameter;
        }
    }

    @Nonnull
    Method getNativeMethod();

    @Nonnull
    String getName();

    @Nonnull
    Class<?> getDeclaringClass();

    @Nonnull
    TypeToken<?> getReturnType();

    @Nonnull
    List<MethodParameter> getParameters();

    @Nonnull
    Collection<Annotation> getAnnotations();

    @Nullable
    <T extends Annotation> T getAnnotation( @Nonnull Class<T> annotationType );

    @Nonnull
    <T extends Annotation> T requireAnnotation( @Nonnull Class<T> annotationType );

    @Nonnull
    Invoker createInvoker( @Nonnull ParameterResolver... resolvers );

    @Nonnull
    Invoker createInvoker( @Nonnull Collection<ParameterResolver> resolvers );

    @Nullable
    Object invoke( @Nullable Object bean, @Nullable Object... args ) throws Exception;

    interface ParameterResolver
    {
        @Nonnull
        Object SKIP = new Object();

        @Nullable
        Object resolve( @Nonnull MethodParameter parameter, @Nonnull Map<String, Object> resolveContext );
    }

    interface Invoker
    {
        @Nonnull
        MethodHandle getMethod();

        @Nonnull
        Invocation resolve( @Nonnull Map<String, Object> resolveContext );
    }

    interface Invocation
    {
        @Nonnull
        Invoker getInvoker();

        @Nonnull
        Object[] getArguments();

        @Nullable
        Object invoke( @Nonnull Object bean ) throws Exception;
    }
}
