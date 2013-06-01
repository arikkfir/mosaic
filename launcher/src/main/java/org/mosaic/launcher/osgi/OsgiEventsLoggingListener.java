package org.mosaic.launcher.osgi;

import javax.annotation.Nonnull;
import org.osgi.framework.*;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Arrays.asList;

/**
 * @author arik
 */
public class OsgiEventsLoggingListener implements FrameworkListener, ServiceListener
{
    private static final Logger OSGI_SVC_LOG = LoggerFactory.getLogger( "org.osgi.service" );

    private static final Logger OSGI_FRWK_LOG = LoggerFactory.getLogger( "org.osgi.framework" );

    @Override
    public void frameworkEvent( @Nonnull FrameworkEvent event )
    {
        Throwable throwable = event.getThrowable();
        String throwableMsg = throwable != null ? throwable.getMessage() : "";

        switch( event.getType() )
        {
            case FrameworkEvent.INFO:
                OSGI_FRWK_LOG.info( "OSGi framework informational has occurred: {}", throwableMsg, throwable );
                break;

            case FrameworkEvent.WARNING:
                OSGI_FRWK_LOG.warn( "OSGi framework warning has occurred: {}", throwableMsg, throwable );
                break;

            case FrameworkEvent.ERROR:
                Bundle bundle = event.getBundle();
                String bstr = bundle == null ? "unknown" : bundle.getSymbolicName() + "-" + bundle.getVersion() + "[" + bundle.getBundleId() + "]";
                OSGI_FRWK_LOG.warn( "OSGi framework error for/from bundle '{}' has occurred:", bstr, throwable );
                break;

            case FrameworkEvent.STARTLEVEL_CHANGED:
                OSGI_FRWK_LOG.info( "OSGi framework start level has been changed to: {}", event.getBundle().adapt( FrameworkStartLevel.class ).getStartLevel() );
                break;
        }
    }

    @Override
    public void serviceChanged( ServiceEvent event )
    {
        if( OSGI_SVC_LOG.isTraceEnabled() )
        {
            ServiceReference<?> sr = event.getServiceReference();
            Bundle bundle = sr.getBundle();
            switch( event.getType() )
            {
                case ServiceEvent.REGISTERED:
                    OSGI_SVC_LOG.trace(
                            "OSGi service of type '{}' registered from '{}-{}[{}]'",
                            asList( ( String[] ) sr.getProperty( Constants.OBJECTCLASS ) ),
                            bundle.getSymbolicName(),
                            bundle.getVersion(),
                            bundle.getBundleId()
                    );
                    break;

                case ServiceEvent.UNREGISTERING:
                    OSGI_SVC_LOG.trace(
                            "OSGi service of type '{}' from from '{}-{}[{}]' was unregistered",
                            asList( ( String[] ) sr.getProperty( Constants.OBJECTCLASS ) ),
                            bundle.getSymbolicName(),
                            bundle.getVersion(),
                            bundle.getBundleId()
                    );
                    break;
            }
        }
    }
}
