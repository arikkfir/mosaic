package org.mosaic.runner.logging;

import org.apache.felix.framework.Logger;
import org.mosaic.runner.util.BundleUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * @author arik
 */
public class FelixLogger extends Logger
{
    private static final String OSGI_LOG_NAME = "org.mosaic.server.osgi.framework";

    private static final String MDC_SR_KEY = "logging-osgi-service-ref";

    private static final String MDC_BUNDLE_KEY = "logging-osgi-bundle";

    @Override
    protected void doLog( Bundle bundle, ServiceReference sr, int level, String msg, Throwable throwable )
    {
        MDC.put( MDC_SR_KEY, sr == null ? null : sr.toString( ) );
        MDC.put( MDC_BUNDLE_KEY, bundle == null ? null : BundleUtils.toString( bundle ) );
        try
        {
            switch( level )
            {
                case LOG_DEBUG:
                    if( throwable != null )
                    {
                        LoggerFactory.getLogger( OSGI_LOG_NAME ).debug( msg, throwable );
                    }
                    else
                    {
                        LoggerFactory.getLogger( OSGI_LOG_NAME ).debug( msg );
                    }
                    break;
                case LOG_ERROR:
                    if( throwable != null )
                    {
                        LoggerFactory.getLogger( OSGI_LOG_NAME ).error( msg, throwable );
                    }
                    else
                    {
                        LoggerFactory.getLogger( OSGI_LOG_NAME ).error( msg );
                    }
                    break;
                case LOG_INFO:
                    if( throwable != null )
                    {
                        LoggerFactory.getLogger( OSGI_LOG_NAME ).info( msg, throwable );
                    }
                    else
                    {
                        LoggerFactory.getLogger( OSGI_LOG_NAME ).info( msg );
                    }
                    break;
                case LOG_WARNING:
                default:
                    if( throwable != null )
                    {
                        LoggerFactory.getLogger( OSGI_LOG_NAME ).warn( msg, throwable );
                    }
                    else
                    {
                        LoggerFactory.getLogger( OSGI_LOG_NAME ).warn( msg );
                    }
            }
        }
        finally
        {
            MDC.remove( MDC_SR_KEY );
            MDC.remove( MDC_BUNDLE_KEY );
        }
    }
}
