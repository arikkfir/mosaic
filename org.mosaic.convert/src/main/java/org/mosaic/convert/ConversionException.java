package org.mosaic.convert;

import org.mosaic.core.util.Nonnull;

/**
 * @author arik
 */
public class ConversionException extends RuntimeException
{
    @Nonnull
    private final Class<?> sourceType;

    @Nonnull
    private final Class<?> targetType;

    public ConversionException( String message,
                                @Nonnull Class<?> sourceType,
                                @Nonnull Class<?> targetType )
    {
        super( "Could not convert from '" + sourceType + "' to '" + targetType + "': " + message );
        this.sourceType = sourceType;
        this.targetType = targetType;
    }

    public ConversionException( String message,
                                Throwable cause,
                                @Nonnull Class<?> sourceType,
                                @Nonnull Class<?> targetType )
    {
        super( "Could not convert from '" + sourceType + "' to '" + targetType + "': " + message, cause );
        this.sourceType = sourceType;
        this.targetType = targetType;
    }

    @Nonnull
    public Class<?> getSourceType()
    {
        return this.sourceType;
    }

    @Nonnull
    public Class<?> getTargetType()
    {
        return this.targetType;
    }
}
