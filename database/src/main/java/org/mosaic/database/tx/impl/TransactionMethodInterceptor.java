package org.mosaic.database.tx.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.database.tx.ReadOnly;
import org.mosaic.database.tx.ReadWrite;
import org.mosaic.lifecycle.annotation.BeanRef;
import org.mosaic.lifecycle.annotation.Service;
import org.mosaic.util.reflection.MethodHandle;
import org.mosaic.util.weaving.MethodInterceptor;
import org.mosaic.util.weaving.MethodInvocation;

/**
 * @author arik
 */
@Service(MethodInterceptor.class)
public class TransactionMethodInterceptor implements MethodInterceptor
{
    private TransactionManagerImpl transactionManager;

    @BeanRef
    public void setTransactionManager( TransactionManagerImpl transactionManager )
    {
        this.transactionManager = transactionManager;
    }

    @Nullable
    @Override
    public Object intercept( @Nonnull MethodInvocation invocation ) throws Exception
    {
        MethodHandle methodHandle = invocation.getMethodHandle();

        ReadOnly readOnly = methodHandle.getAnnotation( ReadOnly.class );
        ReadWrite readWrite = methodHandle.getAnnotation( ReadWrite.class );
        if( readOnly != null && readWrite != null )
        {
            throw new IllegalStateException( "Method '" + methodHandle + "' has both @ReadOnly and @ReadWrite" );
        }
        else if( readOnly == null && readWrite == null )
        {
            return invocation.proceed( invocation.getArguments() );
        }
        else
        {
            this.transactionManager.begin( methodHandle.toString(), readOnly != null );
            try
            {
                Object result = invocation.proceed( invocation.getArguments() );
                this.transactionManager.apply();
                return result;
            }
            catch( Exception e )
            {
                throw this.transactionManager.fail( e );
            }
        }
    }
}
