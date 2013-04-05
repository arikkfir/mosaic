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

    @Nonnull
    private final Object source;

    public ConversionException( String message,
                                @Nonnull TypeToken<?> sourceTypeToken,
                                @Nonnull TypeToken<?> targetTypeToken, @Nonnull Object source )
    {
        super( "Could not convert '{}' from '{}' to '{}': " + message );
        this.sourceTypeToken = sourceTypeToken;
        this.targetTypeToken = targetTypeToken;
        this.source = source;
    }

    public ConversionException( String message,
                                Throwable cause,
                                @Nonnull TypeToken<?> sourceTypeToken,
                                @Nonnull TypeToken<?> targetTypeToken, @Nonnull Object source )
    {
        super( "Could not convert '{}' from '{}' to '{}': " + message, cause );
        this.sourceTypeToken = sourceTypeToken;
        this.targetTypeToken = targetTypeToken;
        this.source = source;
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

    @Nonnull
    public Object getSource()
    {
        return source;
    }
}
