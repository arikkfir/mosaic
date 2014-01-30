package org.mosaic.util.conversion;

import com.google.common.reflect.TypeToken;
import javax.annotation.Nonnull;

/**
 * @author arik
 */
public class ConversionException extends RuntimeException
{
    @Nonnull
    private final TypeToken<?> sourceType;

    @Nonnull
    private final TypeToken<?> targetType;

    public ConversionException( String message,
                                @Nonnull TypeToken<?> sourceType,
                                @Nonnull TypeToken<?> targetType )
    {
        super( "Could not convert from '" + sourceType + "' to '" + targetType + "': " + message );
        this.sourceType = sourceType;
        this.targetType = targetType;
    }

    public ConversionException( String message,
                                Throwable cause,
                                @Nonnull TypeToken<?> sourceType,
                                @Nonnull TypeToken<?> targetType )
    {
        super( "Could not convert from '" + sourceType + "' to '" + targetType + "': " + message, cause );
        this.sourceType = sourceType;
        this.targetType = targetType;
    }

    @Nonnull
    public TypeToken<?> getSourceType()
    {
        return this.sourceType;
    }

    @Nonnull
    public TypeToken<?> getTargetType()
    {
        return this.targetType;
    }
}
