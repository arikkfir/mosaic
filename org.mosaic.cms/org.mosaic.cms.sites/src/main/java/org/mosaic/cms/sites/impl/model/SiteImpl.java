package org.mosaic.cms.sites.impl.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.mosaic.cms.Blueprint;
import org.mosaic.cms.Page;
import org.mosaic.cms.Site;
import org.mosaic.web.HttpApplication;

/**
 * @author arik
 */
public class SiteImpl extends BaseModel implements Site
{
    private final HttpApplication application;

    private Map<String, BlueprintImpl> blueprints = new HashMap<>();

    private Map<String, PageImpl> pages = new HashMap<>();

    public SiteImpl( HttpApplication application )
    {
        this.application = application;
    }

    @Override
    public BlueprintImpl getBlueprint( String name )
    {
        return this.blueprints.get( name );
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public Collection<Blueprint> getBlueprints()
    {
        return ( Collection<Blueprint> ) this.blueprints.values();
    }

    public synchronized void addBlueprint( BlueprintImpl blueprint )
    {
        Map<String, BlueprintImpl> newBlueprints = new HashMap<>( this.blueprints );
        newBlueprints.put( blueprint.getName(), blueprint );
        this.blueprints = newBlueprints;
    }

    @Override
    public Page getPage( String name )
    {
        return this.pages.get( name );
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public Collection<Page> getPages()
    {
        return ( Collection<Page> ) this.pages.values();
    }

    public synchronized void addPage( PageImpl page )
    {
        Map<String, PageImpl> newPages = new HashMap<>( this.pages );
        newPages.put( page.getName(), page );
        this.pages = newPages;
    }

    @Override
    public String getName()
    {
        return this.application.getName();
    }

    @Override
    public String getDisplayName()
    {
        return this.application.getDisplayName();
    }
}
