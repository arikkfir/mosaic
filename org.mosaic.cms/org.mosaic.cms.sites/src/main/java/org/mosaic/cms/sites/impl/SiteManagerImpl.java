package org.mosaic.cms.sites.impl;

import org.mosaic.cms.Site;
import org.mosaic.cms.sites.SiteManager;
import org.mosaic.lifecycle.ServiceExport;
import org.mosaic.web.HttpApplication;
import org.springframework.stereotype.Component;

/**
 * @author arik
 */
@Component
@ServiceExport( SiteManager.class )
public class SiteManagerImpl implements SiteManager
{
    @Override
    public Site getSite( String name )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Site getSite( HttpApplication application )
    {
        throw new UnsupportedOperationException();
    }
}
