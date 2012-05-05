package org.mosaic.cms.sites.impl.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.mosaic.cms.Blueprint;
import org.mosaic.cms.Panel;
import org.mosaic.cms.Site;

/**
 * @author arik
 */
public class BlueprintImpl extends BaseModel implements Blueprint
{
    private Site site;

    private Blueprint parent;

    private Map<String, PanelImpl> panels = new HashMap<>();

    @Override
    public Site getSite()
    {
        return this.site;
    }

    public void setSite( Site site )
    {
        this.site = site;
    }

    @Override
    public Blueprint getParent()
    {
        return this.parent;
    }

    public void setParent( Blueprint parent )
    {
        this.parent = parent;
    }

    @Override
    public Panel getPanel( String name )
    {
        return this.panels.get( name );
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public Collection<Panel> getPanels()
    {
        return ( Collection<Panel> ) this.panels.values();
    }

    public synchronized void addPanel( PanelImpl panel )
    {
        Map<String, PanelImpl> newPanels = new HashMap<>( this.panels );
        newPanels.put( panel.getName(), panel );
        this.panels = newPanels;
    }
}
