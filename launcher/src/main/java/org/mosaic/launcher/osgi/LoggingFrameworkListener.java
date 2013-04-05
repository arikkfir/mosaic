package org.mosaic.launcher.osgi;

import javax.annotation.Nonnull;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
public class LoggingFrameworkListener implements FrameworkListener
{
    private static final Logger LOG = LoggerFactory.getLogger( "org.osgi.framework" );

    @Override
    public void frameworkEvent( @Nonnull FrameworkEvent event )
    {
        Throwable throwable = event.getThrowable();
        String throwableMsg = throwable != null ? throwable.getMessage() : "";

        switch( event.getType() )
        {
            case FrameworkEvent.INFO:
                LOG.warn( "OSGi framework informational has occurred: {}", throwableMsg, throwable );
                break;

            case FrameworkEvent.WARNING:
                LOG.warn( "OSGi framework warning has occurred: {}", throwableMsg, throwable );
                break;

            case FrameworkEvent.ERROR:
                Bundle bundle = event.getBundle();
                String bstr = bundle == null ? "unknown" : bundle.getSymbolicName() + "-" + bundle.getVersion() + "[" + bundle.getBundleId() + "]";
                LOG.error( "OSGi framework error for/from bundle '{}' has occurred:", bstr, throwable );
                break;

            case FrameworkEvent.STARTLEVEL_CHANGED:
                LOG.info( "OSGi framework start level has been changed to: {}", event.getBundle().adapt( FrameworkStartLevel.class ).getStartLevel() );
                break;
        }
    }
}
