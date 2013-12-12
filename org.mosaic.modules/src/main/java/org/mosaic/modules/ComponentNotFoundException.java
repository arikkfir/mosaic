package org.mosaic.modules;

import javax.annotation.Nonnull;

/**
 * @author arik
 */
public class ComponentNotFoundException extends ComponentException
{
    public ComponentNotFoundException( @Nonnull Class<?> type, @Nonnull Module module )
    {
        super( "could not find component of type '" + type.getName() + "'", type, module );
    }
}
