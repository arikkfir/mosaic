package org.mosaic.cms.sites;

import org.mosaic.cms.Site;
import org.mosaic.web.HttpApplication;

/**
 * @author arik
 */
public interface SiteManager
{
    Site getSite( String name );

    Site getSite( HttpApplication application );
}
