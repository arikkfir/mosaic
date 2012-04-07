package org.mosaic.server.boot.impl.publish.requirement;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Dictionary;
import java.util.Hashtable;
import org.mosaic.lifecycle.MethodEndpointInfo;
import org.mosaic.server.boot.impl.publish.BundlePublisher;
import org.mosaic.server.boot.impl.publish.requirement.support.AbstractMethodRequirement;
import org.osgi.framework.ServiceRegistration;
import org.springframework.context.ApplicationContext;

/**
 * @author arik
 */
public class MethodEndpointRequirement extends AbstractMethodRequirement implements MethodEndpointInfo {

    private final Class<? extends Annotation> type;

    private ServiceRegistration<MethodEndpointInfo> registration;

    public MethodEndpointRequirement( BundlePublisher publisher,
                                      String beanName,
                                      Method targetMethod,
                                      Class<? extends Annotation> type ) {
        super( publisher, beanName, targetMethod );
        this.type = type;
    }

    @Override
    public boolean open() throws Exception {
        super.open();
        return true;
    }

    @Override
    public void onPublish( ApplicationContext applicationContext ) throws Exception {
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put( TYPE, this.type.getName() );
        properties.put( SHORT_TYPE, this.type.getSimpleName() );
        this.registration = getBundleContext().registerService( MethodEndpointInfo.class, this, properties );
    }

    @Override
    public void revert() throws Exception {
        this.registration.unregister();
    }

    @Override
    public Class<? extends Annotation> getType() {
        return this.type;
    }
}
