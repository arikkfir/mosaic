package org.mosaic.modules;

import javax.annotation.Nonnull;

/**
 * @author arik
 */
public final class ModuleEvent
{
    @Nonnull
    private final Module module;

    @Nonnull
    private final ModuleEventType eventType;

    public ModuleEvent( @Nonnull Module module, @Nonnull ModuleEventType eventType )
    {
        this.module = module;
        this.eventType = eventType;
    }

    @Nonnull
    public Module getModule()
    {
        return this.module;
    }

    @Nonnull
    public ModuleEventType getEventType()
    {
        return this.eventType;
    }
}
