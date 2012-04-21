package org.mosaic.server.boot.impl;

import org.mosaic.logging.Logger;
import org.mosaic.logging.LoggerFactory;
import org.mosaic.osgi.util.BundleUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * @author arik
 */
public class MosaicLogListener implements LogListener, ServiceTrackerCustomizer<LogReaderService, LogReaderService> {

    private static final Logger LOG = LoggerFactory.getLogger( MosaicLogListener.class );

    private static final Logger OSGI_LOG = LoggerFactory.getLogger( "org.mosaic.osgi.log" );

    private final BundleContext bundleContext;

    private ServiceTracker<LogReaderService, LogReaderService> logReaderServiceTracker;

    public MosaicLogListener( BundleContext bundleContext ) {
        this.bundleContext = bundleContext;
    }

    public void open() {
        LOG.debug( "Opened OSGi events logger" );
        this.logReaderServiceTracker = new ServiceTracker<>( bundleContext, LogReaderService.class, this );
        this.logReaderServiceTracker.open();
    }

    public void close() {
        this.logReaderServiceTracker.close();
        this.logReaderServiceTracker = null;
        LOG.debug( "Closed OSGi events logger" );
    }

    @Override
    public void logged( LogEntry logEntry ) {
        String msg = logEntry.getMessage() + "\n" + "Bundle: {}\n" + "Service: {}\n";
        switch( logEntry.getLevel() ) {
            case 1:
                OSGI_LOG.error( msg, BundleUtils.toString( logEntry.getBundle() ), logEntry.getServiceReference(), logEntry.getException() );
                break;
            case 2:
                OSGI_LOG.warn( msg, BundleUtils.toString( logEntry.getBundle() ), logEntry.getServiceReference(), logEntry.getException() );
                break;
            case 3:
                OSGI_LOG.info( msg, BundleUtils.toString( logEntry.getBundle() ), logEntry.getServiceReference(), logEntry.getException() );
                break;
            case 4:
                OSGI_LOG.debug( msg, BundleUtils.toString( logEntry.getBundle() ), logEntry.getServiceReference(), logEntry.getException() );
                break;
        }
    }

    @Override
    public LogReaderService addingService( ServiceReference<LogReaderService> reference ) {
        LogReaderService service = this.bundleContext.getService( reference );
        if( service != null ) {
            service.addLogListener( this );
        }
        return service;
    }

    @Override
    public void modifiedService( ServiceReference<LogReaderService> reference, LogReaderService service ) {
        // no-op
    }

    @Override
    public void removedService( ServiceReference<LogReaderService> reference, LogReaderService service ) {
        if( service != null ) {
            service.removeLogListener( this );
        }
    }
}
