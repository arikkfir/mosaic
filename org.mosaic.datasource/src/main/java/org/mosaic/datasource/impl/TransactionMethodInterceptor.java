package org.mosaic.datasource.impl;

import java.lang.reflect.Method;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.datasource.ReadOnly;
import org.mosaic.datasource.ReadWrite;
import org.mosaic.modules.Component;
import org.mosaic.modules.Service;
import org.mosaic.modules.spi.MethodInterceptor;
import org.mosaic.util.collections.MapEx;
import org.mosaic.util.reflection.MethodAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
@Service
final class TransactionMethodInterceptor implements MethodInterceptor
{
    private static final Logger LOG = LoggerFactory.getLogger( TransactionMethodInterceptor.class );

    @Nonnull
    @Component
    private TransactionManagerImpl transactionManager;

    @Override
    public boolean interestedIn( @Nonnull Method method, @Nonnull MapEx<String, Object> context )
    {
        if( MethodAnnotations.getMetaAnnotation( method, ReadOnly.class ) != null )
        {
            context.put( "readOnly", true );
            return true;
        }

        if( MethodAnnotations.getMetaAnnotation( method, ReadWrite.class ) != null )
        {
            context.put( "readOnly", false );
            return true;
        }

        return false;
    }

    @Nullable
    @Override
    public BeforeInvocationDecision beforeInvocation( @Nonnull BeforeMethodInvocation invocation )
            throws Throwable
    {
        invocation.getInvocationContext().put(
                "tx",
                this.transactionManager.startTransaction(
                        invocation.getMethod().getName(),
                        invocation.getInterceptorContext().find( "readOnly", boolean.class ).get() ) );

        return invocation.continueInvocation();
    }

    @Nullable
    @Override
    public Object afterInvocation( @Nonnull AfterMethodInvocation invocation ) throws Throwable
    {
        TransactionManagerImpl.TransactionImpl transaction = this.transactionManager.requireTransaction();
        try
        {
            transaction.commit();
        }
        finally
        {
            transaction.close();
        }
        return invocation.getReturnValue();
    }

    @Nullable
    @Override
    public Object afterThrowable( @Nonnull AfterMethodException invocation ) throws Throwable
    {
        TransactionManagerImpl.TransactionImpl transaction = this.transactionManager.requireTransaction();
        try
        {
            transaction.rollback();
        }
        catch( Throwable e )
        {
            LOG.error( "Could not rollback transaction '{}': {}", transaction.getName(), e.getMessage(), e );
        }
        finally
        {
            transaction.close();
        }
        throw invocation.getThrowable();
    }
}
