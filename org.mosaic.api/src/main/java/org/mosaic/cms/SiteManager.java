package org.mosaic.cms;

import org.mosaic.web.HttpApplication;

/**
 * @author arik
 */
public interface SiteManager
{
    Site getSite( String name );

    Site getSite( HttpApplication application );
}
