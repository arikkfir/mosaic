package org.mosaic.server.boot.impl.publish.requirement;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Enumeration;
import org.apache.commons.io.FileUtils;
import org.mosaic.server.boot.impl.publish.BundleTracker;
import org.mosaic.server.boot.impl.publish.requirement.support.AbstractRequirement;
import org.mosaic.server.lifecycle.WebModuleInfo;
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

    private final File contentRoot;

    private ServiceRegistration<WebModuleInfo> registration;

    public WebModuleInfoRequirement( BundleTracker tracker )
    {
        super( tracker );

        Bundle bundle = getBundleContext().getBundle();

        String bundleResourcesDir = bundle.getHeaders().get( BUNDLE_RESOURCES_HEADER );
        if( bundleResourcesDir != null )
        {
            this.contentRoot = new File( bundleResourcesDir );
            if( !this.contentRoot.exists() )
            {
                LOG.warn( "The '{}' header in bundle '{}' does not exist - auto-reload will not happen for this bundle.", BUNDLE_RESOURCES_HEADER, getBundleName() );
            }
        }
        else
        {
            Path workDir = tracker.getHome().getWork();
            File webContentsDir = new File( workDir.toFile(), "web-content" );
            File resourcesHome = new File( webContentsDir, getBundleName() );
            LOG.info( "Extracting web content from bundle '{}' to: {}", getBundleName(), resourcesHome );
            try
            {
                if( resourcesHome.exists() )
                {
                    FileUtils.forceDelete( resourcesHome );
                }
                Enumeration<URL> allEntries = getBundleContext().getBundle().findEntries( "/web/", null, true );
                while( allEntries.hasMoreElements() )
                {
                    URL entry = allEntries.nextElement();
                    String path = entry.getPath();
                    if( path.endsWith( "/" ) )
                    {
                        continue;
                    }

                    File targetFile = new File( resourcesHome, path.substring( "/web/".length() ) );
                    FileUtils.forceMkdir( targetFile.getParentFile() );
                    FileUtils.copyURLToFile( entry, targetFile );
                }
                this.contentRoot = resourcesHome;
            }
            catch( IOException e )
            {
                throw new IllegalStateException( String.format( "Cannot extract web content from bundle '%s' to '%s': %s", getBundleName(), resourcesHome, e.getMessage() ), e );
            }
        }

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
    public File getContentRoot()
    {
        return this.contentRoot;
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

