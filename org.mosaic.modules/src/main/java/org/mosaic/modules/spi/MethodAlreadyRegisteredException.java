package org.mosaic.modules.spi;

import java.lang.reflect.Method;
import javax.annotation.Nonnull;

/**
 * @author arik
 */
public class MethodAlreadyRegisteredException extends RuntimeException
{
    private final long id;

    @Nonnull
    private final MethodCache.MethodEntry existingEntry;

    @Nonnull
    private final MethodCache.MethodEntry newEntry;

    public MethodAlreadyRegisteredException( long id,
                                             @Nonnull MethodCache.MethodEntry existingEntry,
                                             @Nonnull MethodCache.MethodEntry newEntry )
    {
        super( "method ID '" + id + "' already used by " + existingEntry + " - cannot register " + newEntry );
        this.id = id;
        this.existingEntry = existingEntry;
        this.newEntry = newEntry;
    }

    public long getId()
    {
        return this.id;
    }

    public long getExistingMethodModuleId()
    {
        return this.existingEntry.getModuleId();
    }

    public long getNewMethodModuleId()
    {
        return this.newEntry.getModuleId();
    }

    @Nonnull
    public Method getExistingMethod()
    {
        try
        {
            return this.existingEntry.getMethod();
        }
        catch( ClassNotFoundException e )
        {
            throw new IllegalStateException( "could not obtain method: " + e.getMessage(), e );
        }
    }

    @Nonnull
    public Method getNewMethod()
    {
        try
        {
            return this.newEntry.getMethod();
        }
        catch( ClassNotFoundException e )
        {
            throw new IllegalStateException( "could not obtain method: " + e.getMessage(), e );
        }
    }
}
