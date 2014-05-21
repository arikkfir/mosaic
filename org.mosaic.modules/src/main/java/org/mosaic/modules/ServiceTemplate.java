package org.mosaic.modules;

import java.lang.annotation.Annotation;
import org.mosaic.core.util.Nonnull;

/**
 * @author arik
 */
public interface ServiceTemplate<Type extends Annotation>
{
    @Nonnull
    Class<?> getTemplate();

    @Nonnull
    Type getType();
}
