package org.mosaic.core.components;

import java.lang.annotation.Annotation;
import org.mosaic.core.util.Nonnull;

/**
 * @author arik
 */
public interface ClassEndpoint<Type extends Annotation>
{
    @Nonnull
    Type getEndpointType();

    @Nonnull
    Class<?> getType();
}
