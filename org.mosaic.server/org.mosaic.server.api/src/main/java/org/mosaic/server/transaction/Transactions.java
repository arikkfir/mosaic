package org.mosaic.server.transaction;

import org.mosaic.logging.Logger;
import org.mosaic.logging.LoggerFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * @author arik
 */
public abstract class Transactions {

    private static final ThreadLocal<TransactionInfo> transactionInfoHolder = new InheritableThreadLocal<>();

    @SuppressWarnings( "UnusedDeclaration" )
    public static void begin( String transactionName, Object transactional ) {
        try {
            new TransactionInfo( transactionName, transactional ).bind();
        } catch( CannotCreateTransactionException e ) {
            throw e;
        } catch( Exception e ) {
            throw new CannotCreateTransactionException( e.getMessage(), e );
        }
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public static void rollback() {
        TransactionInfo tx = transactionInfoHolder.get();
        try {
            tx.rollback();
        } catch( TransactionException e ) {
            Logger logger = LoggerFactory.getLogger( tx.transactionName );
            logger.error( "Could not rollback transaction '{}': {}", tx.transactionName, e.getMessage(), e );
        } finally {
            try {
                tx.restore();
            } catch( Exception e ) {
                Logger logger = LoggerFactory.getLogger( tx.transactionName );
                logger.error( "Could not unbind from transaction '{}': {}", tx.transactionName, e.getMessage(), e );
            }
        }
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public static void finish() {
        TransactionInfo tx = transactionInfoHolder.get();
        try {
            tx.commit();
        } finally {
            tx.restore();
        }
    }

    private static class TransactionInfo {

        private final BundleContext bundleContext;

        private final TransactionManager transactionManager;

        private final ServiceReference<TransactionManager> transactionManagerRef;

        private final DefaultTransactionDefinition transactionDefinition;

        private final String transactionName;

        private TransactionStatus transactionStatus;

        private TransactionInfo oldTransactionInfo;

        public TransactionInfo( String transactionName, Object transactional ) {
            this.bundleContext = FrameworkUtil.getBundle( transactional.getClass() ).getBundleContext();
            if( bundleContext == null ) {
                throw new CannotCreateTransactionException( "Class '" + transactional.getClass() + "' is not part of any bundle" );
            }

            //TODO 4/17/12: fetch appropriate TransactionManager based on the given txSourceName parameter
            this.transactionManagerRef = bundleContext.getServiceReference( TransactionManager.class );
            if( this.transactionManagerRef == null ) {
                throw new TransactionServiceNotAvailableException( bundleContext );
            }

            this.transactionManager = bundleContext.getService( this.transactionManagerRef );
            if( this.transactionManager == null ) {
                bundleContext.ungetService( this.transactionManagerRef );
                throw new TransactionServiceNotAvailableException( bundleContext );
            }

            this.transactionName = transactionName;
            this.transactionDefinition = new DefaultTransactionDefinition();
            this.transactionDefinition.setName( this.transactionName );
            this.transactionDefinition.setPropagationBehavior( TransactionDefinition.PROPAGATION_REQUIRED );
        }

        private void bind() {
            this.transactionStatus = this.transactionManager.getTransaction( this.transactionDefinition );
            this.oldTransactionInfo = transactionInfoHolder.get();
            transactionInfoHolder.set( this );
        }

        private void rollback() {
            this.transactionManager.rollback( this.transactionStatus );
        }

        private void commit() {
            this.transactionManager.commit( this.transactionStatus );
        }

        private void restore() {
            transactionInfoHolder.set( this.oldTransactionInfo );
            this.bundleContext.ungetService( this.transactionManagerRef );
        }

        @Override
        public String toString() {
            return this.transactionDefinition.toString();
        }
    }
}
