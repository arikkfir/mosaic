package org.mosaic.server.boot.impl.publish.requirement;

import org.mosaic.logging.Logger;
import org.mosaic.logging.LoggerFactory;
import org.mosaic.server.boot.impl.publish.BundleTracker;
import org.mosaic.server.boot.impl.publish.requirement.support.AbstractBeanRequirement;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.springframework.context.ApplicationContext;

/**
 * @author arik
 */
public class ServiceExportRequirement extends AbstractBeanRequirement {

    private static final Logger LOG = LoggerFactory.getBundleLogger( ServiceExportRequirement.class );

    private final Class<?> apiType;

    private ServiceRegistration registration;

    public ServiceExportRequirement( BundleTracker tracker, String beanName, Class<?> apiType ) {
        super( tracker, beanName );
        this.apiType = apiType;
    }

    @Override
    public String toString() {
        return "ServiceExport[" + this.apiType.getSimpleName() + "/" + getBeanName() + "]";
    }

    @Override
    public int getPriority() {
        return SERVICE_EXPORT_PRIORITY;
    }

    @Override
    protected boolean trackInternal() throws Exception {
        super.trackInternal();
        return true;
    }

    @Override
    protected void publishInternal( ApplicationContext applicationContext ) throws Exception {
        BundleContext bundleContext = getBundleContext();
        if( bundleContext == null ) {
            LOG.warn( "Publishing non-active bundle?? For bundle: {}", getBundleName() );
        } else {
            this.registration = bundleContext.registerService( this.apiType.getName(), getBean( applicationContext ), null );
        }
    }

    @Override
    protected void unpublishInternal() throws Exception {
        if( this.registration != null ) {
            try {
                this.registration.unregister();
            } catch( IllegalStateException ignore ) {
            }
        }
    }
}
