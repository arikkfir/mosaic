package org.mosaic.it.runner;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

/**
 * @author arik
 */
public final class ServerVersion
{
    private static String version;

    static
    {
        URL versionPropertiesUrl = ServerVersion.class.getClassLoader().getResource( "/src/test/resources/version.properties" );
        if( versionPropertiesUrl == null )
        {
            throw new IllegalStateException( "Could not find 'version.properties' resource in class-path" );
        }

        try
        {
            Properties properties = new Properties();
            properties.load( versionPropertiesUrl.openStream() );
            ServerVersion.version = properties.getProperty( "version" );
        }
        catch( IOException e )
        {
            throw new IllegalStateException( "Could not load server version: " + e.getMessage(), e );
        }
    }

    public static String getVersion()
    {
        return version;
    }

    private ServerVersion()
    {
    }
}
