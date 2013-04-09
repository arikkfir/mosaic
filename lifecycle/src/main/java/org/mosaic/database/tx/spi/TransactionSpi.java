package org.mosaic.database.tx.spi;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.database.tx.NoTransactionException;
import org.mosaic.database.tx.TransactionManager;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author arik
 */
public final class TransactionSpi implements InitializingBean, DisposableBean
{
    private static TransactionSpi instance;

    @SuppressWarnings("UnusedDeclaration")
    public static void begin( @Nonnull String name, boolean readOnly )
    {
        getTransactionManager().begin( name, readOnly );
    }

    @SuppressWarnings("UnusedDeclaration")
    public static void fail( @Nonnull Exception exception ) throws NoTransactionException
    {
        getTransactionManager().fail( exception );
    }

    @SuppressWarnings("UnusedDeclaration")
    public static void apply() throws Exception
    {
        getTransactionManager().apply();
    }

    private static TransactionManager getTransactionManager()
    {
        TransactionSpi spi = TransactionSpi.instance;
        if( spi == null )
        {
            throw new IllegalStateException( "Transaction SPI has not been created" );
        }

        ServiceTracker<TransactionManager, TransactionManager> tracker = spi.txMgrTracker;
        if( tracker == null )
        {
            throw new IllegalStateException( "Transaction SPI has not been created" );
        }

        TransactionManager txMgr = tracker.getService();
        if( txMgr == null )
        {
            throw new IllegalStateException( "Transaction Manager is not available" );
        }
        return txMgr;
    }

    @Nullable
    private ServiceTracker<TransactionManager, TransactionManager> txMgrTracker;

    public TransactionSpi( @Nonnull BundleContext bundleContext )
    {
        if( TransactionSpi.instance != null )
        {
            throw new IllegalStateException( "Transaction SPI has already been created!" );
        }

        this.txMgrTracker = new ServiceTracker<>( bundleContext, TransactionManager.class, null );
        TransactionSpi.instance = this;
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        if( this.txMgrTracker != null )
        {
            this.txMgrTracker.open();
        }
    }

    @Override
    public void destroy() throws Exception
    {
        if( this.txMgrTracker != null )
        {
            this.txMgrTracker.close();
        }
    }
}
