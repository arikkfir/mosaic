package org.mosaic.launcher.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.sift.Discriminator;
import ch.qos.logback.core.spi.ContextAwareBase;
import java.util.Map;
import javax.annotation.Nonnull;

/**
 * @author arik
 */
public class JobMDCDiscriminator extends ContextAwareBase implements Discriminator<ILoggingEvent>
{
    private static final String UNKNOWN = "unknown";

    private boolean started = false;

    /**
     * Return the value associated with an MDC entry designated by the Key
     * property. If that value is null, then return the value assigned to the
     * DefaultValue property.
     */
    public String getDiscriminatingValue( @Nonnull ILoggingEvent event )
    {
        // http://jira.qos.ch/browse/LBCLASSIC-213
        Map<String, String> mdcMap = event.getMDCPropertyMap();
        if( mdcMap == null )
        {
            return UNKNOWN;
        }

        String jobType = mdcMap.get( "jobType" );
        if( jobType == null )
        {
            return UNKNOWN;
        }

        String jobExecutionId = mdcMap.get( "jobExecutionId" );
        if( jobExecutionId == null )
        {
            return UNKNOWN;
        }

        return jobType + "/" + jobExecutionId;
    }

    public boolean isStarted()
    {
        return started;
    }

    public void start()
    {
        started = true;
    }

    public void stop()
    {
        started = false;
    }

    @Nonnull
    public String getKey()
    {
        return "jobId";
    }
}
