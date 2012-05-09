package org.mosaic.server.cms.impl.model;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FilenameUtils;
import org.mosaic.cms.Blueprint;
import org.mosaic.cms.Panel;
import org.mosaic.cms.Site;
import org.mosaic.server.util.xml.DomUtils;
import org.w3c.dom.Element;

import static java.lang.String.format;

/**
 * @author arik
 */
public class BlueprintImpl extends BaseModel implements Blueprint
{
    private final Site site;

    private final Blueprint parent;

    private final Map<String, PanelImpl> panels = new HashMap<>();

    public BlueprintImpl( SiteImpl site, File blueprintFile )
    {
        this.site = site;

        String name = FilenameUtils.getBaseName( blueprintFile.getName() );
        Element blueprintElement;
        try
        {
            blueprintElement = DomUtils.parseDocument( blueprintFile.toURI() ).getDocumentElement();
        }
        catch( Exception e )
        {
            site.addError( format( "Could not read or parse blueprint file '%s': %s", blueprintFile, e.getMessage() ), e );
            this.parent = null;
            return;
        }

        DomModelUtils.setNames( this, name, blueprintElement );
        DomModelUtils.setProperties( this, blueprintElement );
        DomModelUtils.addDataProviders( this, blueprintElement, site.getDataProviderRegistry() );
        DomModelUtils.setSecurityExpression( this, blueprintElement );
        this.parent = blueprintElement.hasAttribute( "parent" )
                      ? site.getBlueprint( blueprintElement.getAttribute( "parent" ) )
                      : null;

        for( Element panelElement : DomUtils.getChildElements( blueprintElement, "panel" ) )
        {
            PanelImpl panel = new PanelImpl( panelElement, site.getDataProviderRegistry() );
            this.panels.put( panel.getName(), panel );
        }
    }

    @Override
    public Site getSite()
    {
        return this.site;
    }

    @Override
    public Blueprint getParent()
    {
        return this.parent;
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
        return ( Collection ) this.panels.values();
    }
}
