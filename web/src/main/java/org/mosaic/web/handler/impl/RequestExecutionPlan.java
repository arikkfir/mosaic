package org.mosaic.web.handler.impl;

import javax.annotation.Nonnull;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.expression.ExpressionParser;
import org.mosaic.web.handler.impl.action.ExceptionHandler;
import org.mosaic.web.handler.impl.action.Handler;
import org.mosaic.web.handler.impl.action.Interceptor;
import org.mosaic.web.request.WebRequest;

/**
 * @author arik
 */
public interface RequestExecutionPlan
{
    @Nonnull
    WebRequest getRequest();

    @Nonnull
    ExpressionParser getExpressionParser();

    void addInterceptor( @Nonnull Interceptor interceptor, @Nonnull MapEx<String, Object> context );

    void addHandler( @Nonnull Handler handler, @Nonnull MapEx<String, Object> context );

    void addExceptionHandler( @Nonnull ExceptionHandler exceptionHandler, @Nonnull MapEx<String, Object> context );

    boolean canHandle();

    void execute();
}
