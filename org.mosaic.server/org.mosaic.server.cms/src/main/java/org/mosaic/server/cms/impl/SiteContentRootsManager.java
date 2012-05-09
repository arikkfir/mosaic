package org.mosaic.server.cms.impl;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.annotation.PostConstruct;
import org.mosaic.lifecycle.ServiceBind;
import org.mosaic.lifecycle.ServiceUnbind;
import org.mosaic.server.cms.impl.model.SiteImpl;
import org.mosaic.server.lifecycle.WebModuleInfo;
import org.mosaic.web.HttpApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.Expression;
import org.springframework.stereotype.Component;

/**
 * @author arik
 */
@Component
public class SiteContentRootsManager
{
    private Set<WebModuleInfo> webModules = new HashSet<>();

    private Set<HttpApplication> applications = new HashSet<>();

    private boolean initialized = false;

    private DataProviderRegistry dataProviderRegistry;

    @Autowired
    public void setDataProviderRegistry( DataProviderRegistry dataProviderRegistry )
    {
        this.dataProviderRegistry = dataProviderRegistry;
    }

    @ServiceBind
    public synchronized void httpApplicationAddedOrChanged( HttpApplication application )
    {
        if( !this.applications.contains( application ) )
        {
            Set<HttpApplication> newApplications = new HashSet<>( this.applications );
            newApplications.add( application );
            this.applications = newApplications;
        }

        if( this.initialized )
        {
            buildSiteForApp( application );
        }
    }

    @ServiceUnbind
    public synchronized void httpApplicationRemoved( HttpApplication application )
    {
        application.remove( SiteImpl.class.getName() );

        Set<HttpApplication> newApplications = new HashSet<>( this.applications );
        newApplications.remove( application );
        this.applications = newApplications;
    }

    @ServiceBind
    public synchronized void webModuleAdded( WebModuleInfo webModuleInfo )
    {
        Set<WebModuleInfo> newWebModules = new HashSet<>( this.webModules );
        newWebModules.add( webModuleInfo );
        this.webModules = newWebModules;

        if( this.initialized )
        {
            Expression filter = webModuleInfo.getApplicationFilter();
            for( HttpApplication application : this.applications )
            {
                if( filter == null || filterMatches( filter, application ) )
                {
                    buildSiteForApp( application );
                }
            }
        }
    }

    @ServiceUnbind
    public synchronized void webModuleRemoved( WebModuleInfo webModuleInfo )
    {
        Set<WebModuleInfo> newWebModules = new HashSet<>( this.webModules );
        newWebModules.remove( webModuleInfo );
        this.webModules = newWebModules;

        if( this.initialized )
        {
            Expression filter = webModuleInfo.getApplicationFilter();
            for( HttpApplication application : this.applications )
            {
                if( filter == null || filterMatches( filter, application ) )
                {
                    buildSiteForApp( application );
                }
            }
        }
    }

    @PostConstruct
    public void init()
    {
        for( HttpApplication application : this.applications )
        {
            buildSiteForApp( application );
        }
        this.initialized = true;
    }

    private boolean filterMatches( Expression filter, HttpApplication application )
    {
        Boolean value = filter.getValue( application, Boolean.class );
        return value != null && value;
    }

    private void buildSiteForApp( HttpApplication application )
    {
        // find matching modules for this app
        List<WebModuleInfo> matchingModules = new LinkedList<>();
        for( WebModuleInfo module : this.webModules )
        {
            Expression filter = module.getApplicationFilter();
            if( filter == null || filterMatches( filter, application ) )
            {
                matchingModules.add( module );
            }
        }

        if( !matchingModules.isEmpty() )
        {
            // build a site
            application.put( SiteImpl.class.getName(), new SiteImpl( application, this.dataProviderRegistry, matchingModules ) );
        }
        else
        {
            // no site matched - remove any existing attached site
            application.remove( SiteImpl.class.getName() );
        }
    }
}
