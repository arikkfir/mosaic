package org.mosaic.server.boot.impl.publish.requirement;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import org.mosaic.lifecycle.WebModuleInfo;
import org.mosaic.server.boot.impl.publish.BundleTracker;
import org.mosaic.server.boot.impl.publish.requirement.support.AbstractRequirement;
import org.mosaic.util.logging.Logger;
import org.mosaic.util.logging.LoggerFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.springframework.context.ApplicationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * @author arik
 */
public class WebModuleInfoRequirement extends AbstractRequirement implements WebModuleInfo
{
    private static final Logger LOG = LoggerFactory.getLogger( WebModuleInfoRequirement.class );

    public static final String BUNDLE_RESOURCES_HEADER = "Bundle-Resources";

    private final Expression applicationFilter;

    private final URL contentUrl;

    private ServiceRegistration<WebModuleInfo> registration;

    public WebModuleInfoRequirement( BundleTracker tracker, URL contentUrl )
    {
        super( tracker );

        Bundle bundle = getBundleContext().getBundle();

        String bundleResourcesDir = bundle.getHeaders().get( BUNDLE_RESOURCES_HEADER );
        if( bundleResourcesDir != null )
        {
            File file = new File( bundleResourcesDir );
            if( !file.exists() )
            {
                LOG.warn( "The '{}' header in bundle '{}' does not exist - auto-reload will not happen for this bundle.", BUNDLE_RESOURCES_HEADER, getBundleName() );
            }
            else
            {
                try
                {
                    contentUrl = file.toURI().toURL();
                }
                catch( MalformedURLException e )
                {
                    LOG.warn( "The '{}' header in bundle '{}' contains an illegal URL: {}", BUNDLE_RESOURCES_HEADER, getBundleName(), bundleResourcesDir );
                }
            }
        }
        this.contentUrl = contentUrl;

        String applicationFilter = bundle.getHeaders().get( "Application-Filter" );
        if( applicationFilter == null )
        {
            this.applicationFilter = null;
        }
        else
        {
            this.applicationFilter = new SpelExpressionParser().parseExpression( applicationFilter );
        }
    }

    @Override
    public Expression getApplicationFilter()
    {
        return this.applicationFilter;
    }

    @Override
    public URL getContentUrl()
    {
        return contentUrl;
    }

    @Override
    public int getPriority()
    {
        return SERVICE_EXPORT_PRIORITY;
    }

    @Override
    public String toShortString()
    {
        return "WebModuleInfo";
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
            this.registration = bundleContext.registerService( WebModuleInfo.class, this, null );
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

