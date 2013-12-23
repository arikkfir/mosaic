package org.mosaic.modules;

import java.lang.reflect.Type;
import javax.annotation.Nonnull;

/**
 * @author arik
 */
public class ComponentCreateException extends ComponentException
{
    public ComponentCreateException( @Nonnull String message,
                                     @Nonnull Type type,
                                     @Nonnull Module module )
    {
        super( "could not create component of type '" + type + "': " + message, type, module );
    }

    public ComponentCreateException( @Nonnull String message,
                                     @Nonnull Throwable cause,
                                     @Nonnull Type type,
                                     @Nonnull Module module )
    {
        super( "could not create component of type '" + type + "': " + message, cause, type, module );
    }

    public ComponentCreateException( @Nonnull Throwable cause,
                                     @Nonnull Type type,
                                     @Nonnull Module module )
    {
        super( "could not create component of type '" + type + "'", cause, type, module );
    }
}
