package org.mosaic.modules;

import java.lang.annotation.Annotation;
import javax.annotation.Nonnull;

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
