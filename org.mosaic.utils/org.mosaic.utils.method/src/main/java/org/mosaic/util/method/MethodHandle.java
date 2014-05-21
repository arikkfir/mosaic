package org.mosaic.util.method;

import com.google.common.reflect.TypeToken;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;

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

    @Nullable
    <AnnType extends Annotation> AnnType getAnnotation( @Nonnull Class<AnnType> annotationType );

    @Nullable
    <MetaAnnType extends Annotation> MetaAnnType getMetaAnnotation( @Nonnull Class<MetaAnnType> annotationType );

    @Nullable
    <MetaAnnType extends Annotation> Annotation getMetaAnnotationTarget( @Nonnull Class<MetaAnnType> annotationType );
}
