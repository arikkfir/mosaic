package org.mosaic.web.application.impl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.filewatch.WatchEvent;
import org.mosaic.filewatch.annotation.FileWatcher;
import org.mosaic.lifecycle.ModuleListener;
import org.mosaic.lifecycle.ModuleListenerAdapter;
import org.mosaic.lifecycle.annotation.BeanRef;
import org.mosaic.lifecycle.annotation.Service;
import org.mosaic.web.application.WebApplication;
import org.mosaic.web.application.WebApplicationManager;
import org.mosaic.web.application.WebApplicationParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mosaic.filewatch.WatchEvent.*;
import static org.mosaic.filewatch.WatchRoot.APPS;

/**
 * @author arik
 */
@Service({ WebApplicationManager.class, ModuleListener.class })
public class WebApplicationManagerImpl extends ModuleListenerAdapter implements WebApplicationManager
{
    private static final Logger LOG = LoggerFactory.getLogger( WebApplicationManagerImpl.class );

    @Nonnull
    private final Map<String, WebApplicationFactory.WebApplicationImpl> applications = new ConcurrentHashMap<>();

    @Nonnull
    private WebApplicationFactory webApplicationFactory;

    @BeanRef
    public void setWebApplicationFactory( @Nonnull WebApplicationFactory webApplicationFactory )
    {
        this.webApplicationFactory = webApplicationFactory;
    }

    @Nonnull
    @Override
    public Collection<? extends WebApplication> getApplications()
    {
        return this.applications.values();
    }

    @Nullable
    @Override
    public WebApplication getApplication( @Nonnull String name )
    {
        return this.applications.get( name );
    }

    @FileWatcher(root = APPS, pattern = "*.xml", event = FILE_ADDED)
    public synchronized void onAppDescriptorAdded( @Nonnull Path file, @Nonnull WatchEvent event ) throws IOException
    {
        try
        {
            WebApplicationFactory.WebApplicationImpl application = this.webApplicationFactory.parseWebApplication( file );
            this.applications.put( application.getName(), application );
            application.register();
            LOG.info( "Added application '{}' from '{}'", application.getName(), file );
        }
        catch( WebApplicationParseException e )
        {
            LOG.error( "Error parsing web application at '{}': {}", file, e.getMessage(), e );
        }
    }

    @FileWatcher(root = APPS, pattern = "*.xml", event = FILE_MODIFIED)
    public synchronized void onAppDescriptorModified( @Nonnull Path file, @Nonnull WatchEvent event ) throws IOException
    {
        String applicationName = this.webApplicationFactory.getApplicationName( file );
        WebApplicationFactory.WebApplicationImpl application = this.applications.get( applicationName );
        if( application == null )
        {
            LOG.warn( "Web application file '{}' was modified, but Mosaic has no recollection of a web application from that path!", file );
            return;
        }

        try
        {
            application.refresh();
            LOG.info( "Updated application '{}' from '{}'", application.getName(), file );
        }
        catch( Exception e )
        {
            LOG.error( "Error updating web application at '{}': {}", file, e.getMessage(), e );
        }
    }

    @FileWatcher(root = APPS, pattern = "content/*-content.xml", event = { FILE_MODIFIED, FILE_DELETED })
    public synchronized void onAppContentModified( @Nonnull Path file, @Nonnull WatchEvent event ) throws IOException
    {
        Matcher matcher = Pattern.compile( "(.*)\\-content\\.xml" ).matcher( file.getFileName().toString() );
        if( !matcher.matches() )
        {
            return;
        }

        String applicationName = matcher.group( 1 );
        WebApplicationFactory.WebApplicationImpl application = this.applications.get( applicationName );
        if( application == null )
        {
            LOG.warn( "Web content file '{}' was modified, but Mosaic has no recollection of a web application named '{}'!", file, applicationName );
            return;
        }

        try
        {
            application.refresh();
            LOG.info( "Updated application '{}' from '{}'", application.getName(), file );
        }
        catch( Exception e )
        {
            LOG.error( "Error updating web application at '{}': {}", file, e.getMessage(), e );
        }
    }

    @FileWatcher(root = APPS, pattern = "*.xml", event = FILE_DELETED)
    public synchronized void onAppDescriptorDeleted( @Nonnull Path file ) throws IOException
    {
        String appName = this.webApplicationFactory.getApplicationName( file );
        WebApplicationFactory.WebApplicationImpl application = this.applications.remove( appName );
        if( application != null )
        {
            application.unregister();
        }
    }
}
