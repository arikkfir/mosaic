package org.mosaic.validation;

import java.lang.reflect.Method;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.validation.ConstraintViolation;
import javax.validation.ValidationException;

/**
 * @author arik
 */
public class MethodValidationException extends ValidationException
{
    @Nonnull
    private final Method method;

    @Nonnull
    private final Set<ConstraintViolation<Object>> violations;

    public MethodValidationException( @Nonnull Method method, @Nonnull Set<ConstraintViolation<Object>> violations )
    {
        this.method = method;
        this.violations = violations;
    }

    @Override
    public String getMessage()
    {
        StringBuilder msg = new StringBuilder( 100 );
        for( ConstraintViolation<Object> violation : this.violations )
        {
            msg.append( violation.getMessage() ).append( "\n" );
        }
        return msg.toString();
    }

    @Nonnull
    public Method getMethod()
    {
        return this.method;
    }

    @Nonnull
    public Set<ConstraintViolation<Object>> getViolations()
    {
        return this.violations;
    }
}
