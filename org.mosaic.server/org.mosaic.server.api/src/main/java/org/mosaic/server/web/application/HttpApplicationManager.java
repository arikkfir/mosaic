package org.mosaic.server.web.application;

import org.mosaic.web.HttpApplication;

/**
 * @author arik
 */
public interface HttpApplicationManager
{
    HttpApplication getApplication( String name );
}
