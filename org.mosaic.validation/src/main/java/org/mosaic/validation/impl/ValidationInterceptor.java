package org.mosaic.validation.impl;

import java.lang.reflect.Method;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.validation.ConstraintViolation;
import javax.validation.Valid;
import javax.validation.executable.ExecutableValidator;
import org.mosaic.modules.Component;
import org.mosaic.modules.Service;
import org.mosaic.modules.spi.MethodInterceptor;
import org.mosaic.util.collections.MapEx;
import org.mosaic.validation.MethodValidationException;

/**
 * @author arik
 */
@Service
final class ValidationInterceptor implements MethodInterceptor
{
    @Nonnull
    @Component
    private ValidationManager validationManager;

    @Override
    public boolean interestedIn( @Nonnull Method method, @Nonnull MapEx<String, Object> context )
    {
        return method.isAnnotationPresent( Valid.class );
    }

    @Nullable
    @Override
    public BeforeInvocationDecision beforeInvocation( @Nonnull BeforeMethodInvocation invocation )
            throws Throwable
    {
        Object object = invocation.getObject();
        Method method = invocation.getMethod();
        Object[] arguments = invocation.getArguments();

        ExecutableValidator validator = this.validationManager.getValidator().forExecutables();
        Set<ConstraintViolation<Object>> violations = validator.validateParameters( object, method, arguments );
        if( !violations.isEmpty() )
        {
            throw new MethodValidationException( method, violations );
        }
        else
        {
            return invocation.continueInvocation();
        }
    }

    @Nullable
    @Override
    public Object afterInvocation( @Nonnull AfterMethodInvocation invocation ) throws Throwable
    {
        Object object = invocation.getObject();
        Method method = invocation.getMethod();
        Object returnValue = invocation.getReturnValue();

        ExecutableValidator validator = this.validationManager.getValidator().forExecutables();
        Set<ConstraintViolation<Object>> violations = validator.validateReturnValue( object, method, returnValue );
        if( !violations.isEmpty() )
        {
            throw new MethodValidationException( method, violations );
        }
        else
        {
            return returnValue;
        }
    }

    @Nullable
    @Override
    public Object afterThrowable( @Nonnull AfterMethodException invocation ) throws Throwable
    {
        throw invocation.getThrowable();
    }
}
