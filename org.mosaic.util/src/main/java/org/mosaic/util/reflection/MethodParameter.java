package org.mosaic.util.reflection;

import com.google.common.reflect.TypeToken;
import java.lang.annotation.Annotation;
import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.pair.Pair;

/**
 * @author arik
 */
@SuppressWarnings( "UnusedDeclaration" )
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
    <T extends Annotation> T getAnnotation( @Nonnull Class<T> annType );

    @Nonnull
    <T extends Annotation> T requireAnnotation( @Nonnull Class<T> annType );

    <T extends Annotation> boolean hasAnnotation( @Nonnull Class<T> annType );

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
