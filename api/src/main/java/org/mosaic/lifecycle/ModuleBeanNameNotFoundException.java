package org.mosaic.lifecycle;

import javax.annotation.Nonnull;

/**
 * @author arik
 */
public class ModuleBeanNameNotFoundException extends ModuleBeanNotFoundException
{
    @Nonnull
    private final String beanName;

    @Nonnull
    private final Class<?> beanType;

    public ModuleBeanNameNotFoundException( @Nonnull Throwable cause,
                                            @Nonnull Module module,
                                            @Nonnull String beanName,
                                            @Nonnull Class<?> beanType )
    {
        super( "Could not find bean named '" + beanName + "' of type '" + beanType.getName() + "' in module '" + module.getName() + "'", cause, module );
        this.beanName = beanName;
        this.beanType = beanType;
    }

    @Nonnull
    public String getBeanName()
    {
        return this.beanName;
    }

    @Nonnull
    public Class<?> getBeanType()
    {
        return this.beanType;
    }
}
