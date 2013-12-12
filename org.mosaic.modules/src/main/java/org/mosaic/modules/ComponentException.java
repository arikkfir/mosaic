package org.mosaic.modules;

import javax.annotation.Nonnull;

/**
 * @author arik
 */
public class ComponentException extends ModuleException
{
    @Nonnull
    private final Class<?> type;

    public ComponentException( @Nonnull String message, @Nonnull Class<?> type, @Nonnull Module module )
    {
        super( message, module );
        this.type = type;
    }

    public ComponentException( @Nonnull String message,
                               @Nonnull Throwable cause,
                               @Nonnull Class<?> type,
                               @Nonnull Module module )
    {
        super( message, cause, module );
        this.type = type;
    }

    public ComponentException( @Nonnull Throwable cause,
                               @Nonnull Class<?> type,
                               @Nonnull Module module )
    {
        super( cause, module );
        this.type = type;
    }

    @Nonnull
    public final Class<?> getType()
    {
        return this.type;
    }
}
