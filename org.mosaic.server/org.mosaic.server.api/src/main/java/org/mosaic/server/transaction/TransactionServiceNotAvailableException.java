package org.mosaic.server.transaction;

import org.mosaic.osgi.util.BundleUtils;
import org.osgi.framework.BundleContext;
import org.springframework.transaction.CannotCreateTransactionException;

/**
 * @author arik
 */
public class TransactionServiceNotAvailableException extends CannotCreateTransactionException {

    public TransactionServiceNotAvailableException( BundleContext bundleContext ) {
        this( bundleContext, null );
    }

    public TransactionServiceNotAvailableException( BundleContext bundleContext, Throwable cause ) {
        super( "Transaction manager service is not available for bundle '" + BundleUtils.toString( bundleContext ) + "' (or at all)", cause );
    }

}
