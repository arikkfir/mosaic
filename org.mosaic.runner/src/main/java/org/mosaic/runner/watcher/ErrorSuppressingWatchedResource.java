package org.mosaic.runner.watcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
public class ErrorSuppressingWatchedResource implements WatchedResource {

    private static final long MIN_ERROR_COUNT_FOR_QUIET = 5;

    private static final long ERROR_QUIET_TIME = 1000 * 30;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final WatchedResource resource;

    private long lastScanTime;

    private int successiveErrorCount = 0;

    public ErrorSuppressingWatchedResource( WatchedResource resource ) {
        this.resource = resource;
    }

    public ScanResult check() {
        long time = System.currentTimeMillis();

        ScanResult result;
        if( this.successiveErrorCount < MIN_ERROR_COUNT_FOR_QUIET || time - this.lastScanTime >= ERROR_QUIET_TIME ) {

            try {
                result = this.resource.check();
                this.successiveErrorCount = 0;

            } catch( Exception e ) {
                this.logger.error( "Error processing resource '{}': {}", new Object[] {
                        this.resource,
                        e.getMessage(),
                        e
                } );
                this.successiveErrorCount++;
                result = ScanResult.ERROR;

            } finally {
                this.lastScanTime = time;
            }

        } else {

            this.logger.trace( "Skipping scan of '{}' (too many errors, will wait a while before trying again)", this.resource );
            result = ScanResult.SKIPPED;

        }
        return result;
    }

    @Override
    public ScanResult uninstall() {
        try {
            return this.resource.uninstall();
        } catch( Exception e ) {
            this.logger.error( "Error uninstalling resource '{}': {}", new Object[] {
                    this.resource,
                    e.getMessage(),
                    e
            } );
            return ScanResult.ERROR;
        }
    }
}
