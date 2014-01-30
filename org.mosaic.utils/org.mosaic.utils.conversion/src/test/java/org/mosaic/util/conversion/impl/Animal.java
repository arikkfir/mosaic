package org.mosaic.util.conversion.impl;

import javax.annotation.Nonnull;

/**
 * @author arik
 */
public abstract class Animal
{
    @Nonnull
    private final String sound;

    protected Animal( @Nonnull String sound )
    {
        this.sound = sound;
    }

    @Nonnull
    public final String getSound()
    {
        return this.sound;
    }
}
