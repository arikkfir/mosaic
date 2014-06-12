package org.mosaic.core.types;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import org.mosaic.core.util.Nonnull;

/**
 * @author arik
 */
public interface TypeResolver
{
    @Nonnull
    TypeHandle getTypeHandle( @Nonnull Type type );

    @Nonnull
    TypeHandle getTypeHandle( @Nonnull Field field );

    @Nonnull
    TypeHandle getReturnTypeHandle( @Nonnull Method method );

    @Nonnull
    List<TypeHandle> getParametersTypeHandles( @Nonnull Constructor<?> constructor );

    @Nonnull
    List<TypeHandle> getParametersTypeHandles( @Nonnull Method method );
}
