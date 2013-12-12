package org.mosaic.modules;

import javax.annotation.Nonnull;

/**
 * @author arik
 */
public class ComponentDefinitionException extends ComponentException
{
    public ComponentDefinitionException( @Nonnull String message,
                                         @Nonnull Class<?> type,
                                         @Nonnull Module module )
    {
        super( "bad component definition for type '" + type.getName() + "': " + message, type, module );
    }

    public ComponentDefinitionException( @Nonnull String message,
                                         @Nonnull Throwable cause,
                                         @Nonnull Class<?> type,
                                         @Nonnull Module module )
    {
        super( "bad component definition for type '" + type.getName() + "': " + message, cause, type, module );
    }

    public ComponentDefinitionException( @Nonnull Throwable cause,
                                         @Nonnull Class<?> type,
                                         @Nonnull Module module )
    {
        super( "bad component definition for type '" + type.getName() + "'", cause, type, module );
    }
}
