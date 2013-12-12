package org.mosaic.modules;

import javax.annotation.Nonnull;

/**
 * @author arik
 */
public class TooManyComponentsFoundException extends ComponentException
{
    public TooManyComponentsFoundException( @Nonnull Class<?> type, @Nonnull Module module )
    {
        super( "too many components of type '" + type.getName() + "' were found", type, module );
    }
}
