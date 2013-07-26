package org.mosaic.lifecycle;

import javax.annotation.Nonnull;

/**
 * @author arik
 */
public class ModuleBeanTypeNotFoundException extends ModuleBeanNotFoundException
{
    @Nonnull
    private final Class<?> beanType;

    public ModuleBeanTypeNotFoundException( @Nonnull Throwable cause,
                                            @Nonnull Module module,
                                            @Nonnull Class<?> beanType )
    {
        super( "Could not find bean of type '" + beanType.getName() + "' in module '" + module.getName() + "'", cause, module );
        this.beanType = beanType;
    }

    @Nonnull
    public Class<?> getBeanType()
    {
        return this.beanType;
    }
}
