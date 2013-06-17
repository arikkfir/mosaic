package org.mosaic.util.convert;

import com.google.common.reflect.TypeToken;
import javax.annotation.Nonnull;

/**
 * @author arik
 */
public class ConversionException extends RuntimeException
{
    @Nonnull
    private final TypeToken<?> sourceTypeToken;

    @Nonnull
    private final TypeToken<?> targetTypeToken;

    public ConversionException( String message,
                                @Nonnull TypeToken<?> sourceTypeToken,
                                @Nonnull TypeToken<?> targetTypeToken )
    {
        super( "Could not convert from '" + sourceTypeToken + "' to '" + targetTypeToken + "': " + message );
        this.sourceTypeToken = sourceTypeToken;
        this.targetTypeToken = targetTypeToken;
    }

    public ConversionException( String message,
                                Throwable cause,
                                @Nonnull TypeToken<?> sourceTypeToken,
                                @Nonnull TypeToken<?> targetTypeToken )
    {
        super( "Could not convert from '" + sourceTypeToken + "' to '" + targetTypeToken + "': " + message, cause );
        this.sourceTypeToken = sourceTypeToken;
        this.targetTypeToken = targetTypeToken;
    }

    @Nonnull
    public TypeToken<?> getSourceTypeToken()
    {
        return sourceTypeToken;
    }

    @Nonnull
    public TypeToken<?> getTargetTypeToken()
    {
        return targetTypeToken;
    }
}
