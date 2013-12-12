package org.mosaic.modules;

import javax.annotation.Nonnull;

/**
 * @author arik
 */
public class ComponentCreateException extends ComponentException
{
    public ComponentCreateException( @Nonnull String message,
                                     @Nonnull Class<?> type,
                                     @Nonnull Module module )
    {
        super( "could not create component of type '" + type.getName() + "': " + message, type, module );
    }

    public ComponentCreateException( @Nonnull String message,
                                     @Nonnull Throwable cause,
                                     @Nonnull Class<?> type,
                                     @Nonnull Module module )
    {
        super( "could not create component of type '" + type.getName() + "': " + message, cause, type, module );
    }

    public ComponentCreateException( @Nonnull Throwable cause,
                                     @Nonnull Class<?> type,
                                     @Nonnull Module module )
    {
        super( "could not create component of type '" + type.getName() + "'", cause, type, module );
    }
}
