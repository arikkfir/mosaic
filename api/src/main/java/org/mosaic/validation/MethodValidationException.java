package org.mosaic.validation;

import java.util.Collection;
import javax.annotation.Nonnull;
import org.mosaic.util.reflection.MethodHandle;

/**
 * @author arik
 */
public class MethodValidationException extends RuntimeException
{
    @Nonnull
    private final MethodHandle methodHandle;

    @Nonnull
    private final Collection<String> violations;

    public MethodValidationException( @Nonnull MethodHandle methodHandle,
                                      @Nonnull Collection<String> violations )
    {
        super( "method validation failed" );
        this.methodHandle = methodHandle;
        this.violations = violations;
    }

    @Nonnull
    public MethodHandle getMethodHandle()
    {
        return methodHandle;
    }

    @Nonnull
    public Collection<String> getViolations()
    {
        return violations;
    }
}
