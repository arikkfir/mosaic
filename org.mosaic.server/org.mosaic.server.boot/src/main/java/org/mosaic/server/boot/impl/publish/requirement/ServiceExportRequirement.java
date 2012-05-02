package org.mosaic.server.boot.impl.publish.requirement;

import java.util.Dictionary;
import java.util.Hashtable;
import org.mosaic.server.boot.impl.publish.BundleTracker;
import org.mosaic.server.boot.impl.publish.requirement.support.AbstractBeanRequirement;
import org.mosaic.util.logging.Logger;
import org.mosaic.util.logging.LoggerFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.springframework.context.ApplicationContext;

/**
 * @author arik
 */
public class ServiceExportRequirement extends AbstractBeanRequirement
{

    private static final Logger LOG = LoggerFactory.getLogger( ServiceExportRequirement.class );

    private final Class<?> apiType;

    private final int ranking;

    private ServiceRegistration registration;

    public ServiceExportRequirement( BundleTracker tracker, String beanName, Class<?> apiType, int ranking )
    {
        super( tracker, beanName );
        this.apiType = apiType;
        this.ranking = ranking;
    }

    @Override
    public String toString()
    {
        return "ServiceExport[" + this.apiType.getSimpleName() + "/" + getBeanName() + "]";
    }

    @Override
    public int getPriority()
    {
        return SERVICE_EXPORT_PRIORITY;
    }

    @Override
    public String toShortString()
    {
        return "Export as '" + this.apiType.getSimpleName() + "'";
    }

    @Override
    protected boolean trackInternal() throws Exception
    {
        super.trackInternal();
        return true;
    }

    @Override
    protected void publishInternal( ApplicationContext applicationContext ) throws Exception
    {
        BundleContext bundleContext = getBundleContext();
        if( bundleContext == null )
        {
            LOG.warn( "Publishing non-active bundle?? For bundle: {}", getBundleName() );
        }
        else
        {
            Dictionary<String, Object> props = new Hashtable<>();
            props.put( Constants.SERVICE_RANKING, this.ranking );
            this.registration =
                    bundleContext.registerService( this.apiType.getName(), getBean( applicationContext ), props );
        }
    }

    @Override
    protected void unpublishInternal() throws Exception
    {
        if( this.registration != null )
        {
            try
            {
                this.registration.unregister();
            }
            catch( IllegalStateException ignore )
            {
            }
        }
    }
}
