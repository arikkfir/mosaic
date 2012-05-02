package org.mosaic.server.boot.impl.publish.requirement;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Dictionary;
import java.util.Hashtable;
import org.mosaic.lifecycle.MethodEndpointInfo;
import org.mosaic.server.boot.impl.publish.BundleTracker;
import org.mosaic.server.boot.impl.publish.requirement.support.AbstractMethodRequirement;
import org.mosaic.server.osgi.util.BundleUtils;
import org.mosaic.util.logging.Logger;
import org.mosaic.util.logging.LoggerFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.springframework.context.ApplicationContext;

import static org.osgi.framework.Constants.SERVICE_RANKING;

/**
 * @author arik
 */
public class MethodEndpointRequirement extends AbstractMethodRequirement implements MethodEndpointInfo
{

    private static final Logger LOG = LoggerFactory.getLogger( MethodEndpointRequirement.class );

    private final Annotation type;

    private final int ranking;

    private ServiceRegistration<MethodEndpointInfo> registration;

    private ApplicationContext applicationContext;

    public MethodEndpointRequirement( BundleTracker tracker,
                                      String beanName,
                                      Method targetMethod,
                                      Annotation type,
                                      int ranking )
    {
        super( tracker, beanName, targetMethod );
        this.type = type;
        this.ranking = ranking;
    }

    @Override
    public String toString()
    {
        return "MethodEndpoint[@" +
               this.type.annotationType().getSimpleName() +
               "/" +
               getTargetMethod().getName() +
               "/" +
               getBeanName() +
               "]";
    }

    @Override
    public int getPriority()
    {
        return SERVICE_EXPORT_PRIORITY;
    }

    @Override
    public String toShortString()
    {
        return "Export '" + getTargetMethod().getName() + "' as @" + this.type.annotationType().getSimpleName();
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
            LOG.warn( "Bundle being published when not active?? For bundle: {}", getBundleName() );
        }
        else
        {
            this.applicationContext = applicationContext;

            Dictionary<String, Object> properties = new Hashtable<>();
            properties.put( TYPE, this.type.annotationType().getName() );
            properties.put( SHORT_TYPE, this.type.annotationType().getSimpleName() );
            properties.put( METHOD_NAME, getTargetMethod().getName() );
            properties.put( SERVICE_RANKING, this.ranking );
            this.registration = bundleContext.registerService( MethodEndpointInfo.class, this, properties );
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
        this.applicationContext = null;
    }

    @Override
    public String getOrigin()
    {
        return BundleUtils.toString( getBundleContext() );
    }

    @Override
    public boolean isOfType( Class<? extends Annotation> annotationType )
    {
        return this.type.annotationType().equals( annotationType );
    }

    @Override
    public Annotation getType()
    {
        return this.type;
    }

    @Override
    public Method getMethod()
    {
        return getTargetMethod();
    }

    @Override
    public Object invoke( Object... arguments ) throws InvocationTargetException, IllegalAccessException
    {
        ApplicationContext applicationContext = this.applicationContext;
        if( applicationContext == null )
        {
            throw new IllegalStateException( "Bundle '" + getBundleName() + "' is closed" );
        }
        else
        {
            return getTargetMethod().invoke( getBean( applicationContext ), arguments );
        }
    }
}
