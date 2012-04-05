package org.mosaic.server.boot.impl.publish.requirement;

import org.mosaic.server.boot.impl.publish.BundlePublisher;
import org.mosaic.server.boot.impl.publish.requirement.support.AbstractBeanRequirement;
import org.osgi.framework.ServiceRegistration;
import org.springframework.context.ApplicationContext;

/**
 * @author arik
 */
public class ServiceExportRequirement extends AbstractBeanRequirement {

    private final Class<?> apiType;

    private ServiceRegistration registration;

    public ServiceExportRequirement( BundlePublisher publisher, String beanName, Class<?> apiType ) {
        super( publisher, beanName );
        this.apiType = apiType;
    }

    @Override
    public boolean open() throws Exception {
        super.open();
        return true;
    }

    @Override
    public void applyInitial( ApplicationContext applicationContext ) throws Exception {
        this.registration = getBundleContext().registerService(
                this.apiType.getName(), getBean( applicationContext ), null );
    }

    @Override
    public void revert() throws Exception {
        if( this.registration != null ) {
            this.registration.unregister();
        }
    }
}
