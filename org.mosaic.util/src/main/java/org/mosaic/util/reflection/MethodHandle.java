package org.mosaic.util.reflection;

import com.google.common.reflect.TypeToken;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
@SuppressWarnings( "UnusedDeclaration" )
public interface MethodHandle
{
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

    boolean hasAnnotation( @Nonnull Class<? extends Annotation> type );

    boolean hasAnnotation( @Nonnull Class<? extends Annotation> type, boolean deep );

    @Nullable
    <T extends Annotation> T getAnnotation( @Nonnull Class<T> annotationType );

    @Nullable
    <T extends Annotation> T getAnnotation( @Nonnull Class<T> annotationType, boolean deep );

    @Nullable
    Annotation findMetaAnnotationTarget( @Nonnull Class<? extends Annotation> metaAnnotationType );

    @Nonnull
    <T extends Annotation> T requireAnnotation( @Nonnull Class<T> annotationType );

    @Nonnull
    <T extends Annotation> T requireAnnotation( @Nonnull Class<T> annotationType, boolean deep );
}
