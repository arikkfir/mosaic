package org.mosaic.database.tx.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.database.tx.TransactionCreationException;
import org.mosaic.database.tx.annotation.ReadOnly;
import org.mosaic.database.tx.annotation.ReadWrite;
import org.mosaic.lifecycle.annotation.BeanRef;
import org.mosaic.lifecycle.annotation.Service;
import org.mosaic.util.reflection.MethodHandle;
import org.mosaic.util.weaving.MethodInterceptor;

/**
 * @author arik
 */
@Service(MethodInterceptor.class)
public class TransactionalMethodInterceptor implements MethodInterceptor
{
    @Nonnull
    private TransactionManagerImpl transactionManager;

    @BeanRef
    public void setTransactionManager( @Nonnull TransactionManagerImpl transactionManager )
    {
        this.transactionManager = transactionManager;
    }

    @Nullable
    @Override
    public Object intercept( @Nonnull MethodInvocation invocation ) throws Throwable
    {
        MethodHandle methodHandle = invocation.getMethodHandle();

        // check method return value if it is annotated with @Nonnull
        boolean readOnly = methodHandle.hasAnnotation( ReadOnly.class );
        boolean readWrite = methodHandle.hasAnnotation( ReadWrite.class );
        if( readOnly && readWrite )
        {
            throw new TransactionCreationException( "Method '" + methodHandle + "' has both @ReadOnly and @ReadWrite" );
        }
        else if( readOnly || readWrite )
        {
            this.transactionManager.begin( methodHandle.toString(), readOnly );
            try
            {
                Object result = invocation.proceed();
                this.transactionManager.apply();
                return result;
            }
            catch( Exception e )
            {
                this.transactionManager.fail( e );
                throw e;
            }
        }
        else
        {
            return invocation.proceed();
        }
    }
}
