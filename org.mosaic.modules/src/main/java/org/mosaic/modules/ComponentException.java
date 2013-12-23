package org.mosaic.modules;

import java.lang.reflect.Type;
import javax.annotation.Nonnull;

/**
 * @author arik
 */
public class ComponentException extends ModuleException
{
    @Nonnull
    private final Type type;

    public ComponentException( @Nonnull String message, @Nonnull Type type, @Nonnull Module module )
    {
        super( message, module );
        this.type = type;
    }

    public ComponentException( @Nonnull String message,
                               @Nonnull Throwable cause,
                               @Nonnull Type type,
                               @Nonnull Module module )
    {
        super( message, cause, module );
        this.type = type;
    }

    public ComponentException( @Nonnull Throwable cause,
                               @Nonnull Type type,
                               @Nonnull Module module )
    {
        super( cause, module );
        this.type = type;
    }

    @Nonnull
    public final Type getType()
    {
        return this.type;
    }
}
