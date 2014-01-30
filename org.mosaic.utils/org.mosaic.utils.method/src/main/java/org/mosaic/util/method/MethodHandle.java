package org.mosaic.util.method;

import com.google.common.base.Optional;
import com.google.common.reflect.TypeToken;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;

/**
 * @author arik
 */
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

    @Nonnull
    <AnnType extends Annotation> Optional<AnnType> getAnnotation( @Nonnull Class<AnnType> annotationType );

    @Nonnull
    <MetaAnnType extends Annotation> Optional<MetaAnnType> getMetaAnnotation( @Nonnull Class<MetaAnnType> annotationType );

    @Nonnull
    <MetaAnnType extends Annotation> Optional<Annotation> getMetaAnnotationTarget( @Nonnull Class<MetaAnnType> annotationType );
}
