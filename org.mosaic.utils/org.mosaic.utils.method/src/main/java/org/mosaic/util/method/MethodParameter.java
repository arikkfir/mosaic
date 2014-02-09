package org.mosaic.util.method;

import com.google.common.reflect.TypeToken;
import java.lang.annotation.Annotation;
import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;

/**
 * @author arik
 */
public interface MethodParameter
{
    @Nonnull
    MethodHandle getMethod();

    @Nonnull
    String getName();

    int getIndex();

    @Nonnull
    TypeToken<?> getType();

    @Nonnull
    Collection<Annotation> getAnnotations();

    @Nullable
    <AnnType extends Annotation> AnnType getAnnotation( @Nonnull Class<AnnType> annotationType );

    @Nullable
    <MetaAnnType extends Annotation> MetaAnnType getMetaAnnotation( @Nonnull Class<MetaAnnType> annotationType );

    @Nullable
    <MetaAnnType extends Annotation> Annotation getMetaAnnotationTarget( @Nonnull Class<MetaAnnType> annotationType );

    boolean isPrimitive();

    boolean isArray();

    boolean isList();

    boolean isSet();

    boolean isCollection();

    @Nullable
    TypeToken<?> getCollectionItemType();

    boolean isMapEx();

    boolean isMap();

    @Nullable
    Pair<TypeToken<?>, TypeToken<?>> getMapType();

    boolean isProperties();
}
