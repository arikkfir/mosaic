package org.mosaic.modules;

import java.lang.reflect.Type;
import javax.annotation.Nonnull;

/**
 * @author arik
 */
public class ComponentDefinitionException extends ComponentException
{
    public ComponentDefinitionException( @Nonnull String message,
                                         @Nonnull Type type,
                                         @Nonnull Module module )
    {
        super( "bad component definition for type '" + type + "' in module " + module + ": " + message, type, module );
    }

    public ComponentDefinitionException( @Nonnull String message,
                                         @Nonnull Throwable cause,
                                         @Nonnull Type type,
                                         @Nonnull Module module )
    {
        super( "bad component definition for type '" + type + "' in module " + module + ": " + message, cause, type, module );
    }

    public ComponentDefinitionException( @Nonnull Throwable cause,
                                         @Nonnull Type type,
                                         @Nonnull Module module )
    {
        super( "bad component definition for type '" + type + "' in module " + module, cause, type, module );
    }
}
