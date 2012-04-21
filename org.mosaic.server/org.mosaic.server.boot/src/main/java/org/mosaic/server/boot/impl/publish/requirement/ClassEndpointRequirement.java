package org.mosaic.server.boot.impl.publish.requirement;

import java.lang.annotation.Annotation;
import java.util.Dictionary;
import java.util.Hashtable;
import org.mosaic.lifecycle.ClassEndpointInfo;
import org.mosaic.logging.Logger;
import org.mosaic.logging.LoggerFactory;
import org.mosaic.osgi.util.BundleUtils;
import org.mosaic.server.boot.impl.publish.BundleTracker;
import org.mosaic.server.boot.impl.publish.requirement.support.AbstractBeanRequirement;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.springframework.context.ApplicationContext;

/**
 * @author arik
 */
public class ClassEndpointRequirement extends AbstractBeanRequirement implements ClassEndpointInfo {

    private static final Logger LOG = LoggerFactory.getLogger( ClassEndpointRequirement.class );

    private final Annotation type;

    private final Class<?> classType;

    private ServiceRegistration<ClassEndpointInfo> registration;

    private ApplicationContext applicationContext;

    public ClassEndpointRequirement( BundleTracker tracker, String beanName, Annotation type, Class<?> classType ) {
        super( tracker, beanName );
        this.type = type;
        this.classType = classType;
    }

    @Override
    public String toString() {
        return "ClassEndpoint[" + this.classType.getSimpleName() + "@" + this.type.annotationType().getSimpleName() + "/" + getBeanName() + "]";
    }

    @Override
    public int getPriority() {
        return SERVICE_EXPORT_PRIORITY;
    }

    @Override
    public String toShortString() {
        return "ClassEndpoint[" + getBeanName() + "]";
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
            LOG.warn( "Bundle being published when not active?? For bundle: {}", getBundleName() );
        } else {
            this.applicationContext = applicationContext;

            Dictionary<String, Object> properties = new Hashtable<>();
            properties.put( TYPE, this.type.annotationType().getName() );
            properties.put( SHORT_TYPE, this.type.annotationType().getSimpleName() );
            properties.put( CLASS_NAME, this.classType.getName() );
            this.registration = bundleContext.registerService( ClassEndpointInfo.class, this, properties );
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
        this.applicationContext = null;
    }

    @Override
    public String getOrigin() {
        return BundleUtils.toString( getBundleContext() );
    }

    @Override
    public boolean isOfType( Class<? extends Annotation> annotationType ) {
        return this.type.annotationType().equals( annotationType );
    }

    @Override
    public Annotation getType() {
        return this.type;
    }

    @Override
    public Class<?> getClassType() {
        return this.classType;
    }

    @Override
    public Object getEndpoint() {
        return getBean( this.applicationContext );
    }
}
