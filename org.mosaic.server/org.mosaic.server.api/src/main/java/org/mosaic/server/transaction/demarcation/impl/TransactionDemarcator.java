package org.mosaic.server.transaction.demarcation.impl;

import org.mosaic.server.transaction.TransactionManager;
import org.mosaic.util.logging.Logger;
import org.mosaic.util.logging.LoggerFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionException;

/**
 * @author arik
 */
public class TransactionDemarcator
{

    private static final ThreadLocal<TransactionInfo> transactionInfoHolder = new InheritableThreadLocal<>( );

    @SuppressWarnings( "UnusedDeclaration" )
    public void begin( String transactionName, Object transactional )
    {
        try
        {
            new TransactionInfo( transactionName, transactional ).bind( );
        }
        catch( CannotCreateTransactionException e )
        {
            throw e;
        }
        catch( Exception e )
        {
            throw new CannotCreateTransactionException( e.getMessage( ), e );
        }
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public void rollback( )
    {
        TransactionInfo tx = transactionInfoHolder.get( );
        try
        {
            tx.rollback( );
        }
        catch( TransactionException e )
        {
            Logger logger = LoggerFactory.getLogger( tx.transactionName );
            logger.error( "Could not rollback transaction '{}': {}", tx.transactionName, e.getMessage( ), e );
        }
        finally
        {
            try
            {
                tx.restore( );
            }
            catch( Exception e )
            {
                Logger logger = LoggerFactory.getLogger( tx.transactionName );
                logger.error( "Could not unbind from transaction '{}': {}", tx.transactionName, e.getMessage( ), e );
            }
        }
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public void finish( )
    {
        TransactionInfo tx = transactionInfoHolder.get( );
        try
        {
            tx.commit( );
        }
        finally
        {
            tx.restore( );
        }
    }

    private static class TransactionInfo
    {

        private final BundleContext bundleContext;

        private final TransactionManager transactionManager;

        private final ServiceReference<TransactionManager> transactionManagerRef;

        private final String transactionName;

        private Object transaction;

        private TransactionInfo oldTransactionInfo;

        public TransactionInfo( String transactionName, Object transactional )
        {
            this.bundleContext = FrameworkUtil.getBundle( transactional.getClass( ) ).getBundleContext( );
            if( bundleContext == null )
            {
                throw new CannotCreateTransactionException( "Class '" +
                                                            transactional.getClass( ) +
                                                            "' is not part of any bundle" );
            }

            //TODO 4/17/12: fetch appropriate TransactionManager based on the given txSourceName parameter
            this.transactionManagerRef = bundleContext.getServiceReference( TransactionManager.class );
            if( this.transactionManagerRef == null )
            {
                throw new TransactionServiceNotAvailableException( bundleContext );
            }

            this.transactionManager = bundleContext.getService( this.transactionManagerRef );
            if( this.transactionManager == null )
            {
                bundleContext.ungetService( this.transactionManagerRef );
                throw new TransactionServiceNotAvailableException( bundleContext );
            }

            this.transactionName = transactionName;
        }

        private void bind( )
        {
            this.transaction = this.transactionManager.begin( this.transactionName );
            this.oldTransactionInfo = transactionInfoHolder.get( );
            transactionInfoHolder.set( this );
        }

        private void rollback( )
        {
            this.transactionManager.rollback( this.transaction );
        }

        private void commit( )
        {
            this.transactionManager.commit( this.transaction );
        }

        private void restore( )
        {
            transactionInfoHolder.set( this.oldTransactionInfo );
            this.bundleContext.ungetService( this.transactionManagerRef );
        }

        @Override
        public String toString( )
        {
            return "Transaction[name=" + this.transactionName + "]";
        }
    }

}
