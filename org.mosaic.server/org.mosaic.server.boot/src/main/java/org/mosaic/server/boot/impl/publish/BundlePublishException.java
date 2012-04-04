package org.mosaic.server.boot.impl.publish;

/**
 * @author arik
 */
public class BundlePublishException extends Exception {

    public BundlePublishException( String message ) {
        super( message );
    }

    public BundlePublishException( String message, Throwable cause ) {
        super( message, cause );
    }
}
