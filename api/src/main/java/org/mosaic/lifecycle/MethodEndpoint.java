package org.mosaic.lifecycle;

import com.google.common.reflect.TypeToken;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.reflection.MethodHandle;
import org.mosaic.util.reflection.MethodParameter;

/**
 * @author arik
 */
public interface MethodEndpoint
{
    @Nonnull
    Module getModule();

    @Nonnull
    Annotation getType();

    @Nonnull
    String getName();

    @Nonnull
    TypeToken<?> getReturnType();

    @Nonnull
    Class<?> getDeclaringType();

    @Nonnull
    List<MethodParameter> getParameters();

    @Nonnull
    Collection<Annotation> getAnnotations();

    @Nullable
    <T extends Annotation> T getAnnotation( @Nonnull Class<T> annotationType );

    @Nullable
    <T extends Annotation> T getAnnotation( @Nonnull Class<T> annotationType,
                                            boolean checkOnClass,
                                            boolean checkOnPackage );

    @Nonnull
    <T extends Annotation> T requireAnnotation( @Nonnull Class<T> annotationType );

    @Nonnull
    <T extends Annotation> T requireAnnotation( @Nonnull Class<T> annotationType,
                                                boolean checkOnClass,
                                                boolean checkOnPackage );

    @Nonnull
    Invoker createInvoker( @Nonnull MethodHandle.ParameterResolver... resolvers );

    @Nonnull
    Invoker createInvoker( @Nonnull Collection<MethodHandle.ParameterResolver> resolvers );

    interface Invoker
    {
        @Nonnull
        MethodEndpoint getMethodEndpoint();

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
        Object invoke() throws Exception;
    }
}
