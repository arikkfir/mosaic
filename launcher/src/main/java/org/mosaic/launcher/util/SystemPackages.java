package org.mosaic.launcher.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mosaic.launcher.util.SystemError.bootstrapError;

/**
 * @author arik
 */
public final class SystemPackages
{
    private static final Logger LOG = LoggerFactory.getLogger( SystemPackages.class );

    private static final Pattern PROPERTY_PATTERN = Pattern.compile( "\\$\\{([^\\}]+)\\}" );

    private static final String extraSystemPackages;

    static
    {
        Map<String, String> mailPackages = new LinkedHashMap<>();
        readPackages( mailPackages, "/extra.systempackages.properties" );
        extraSystemPackages = createSpecForPackages( mailPackages, true );
        LOG.debug( "Extra system packages: {}", SystemPackages.extraSystemPackages );
    }

    public static String getExtraSystemPackages()
    {
        return SystemPackages.extraSystemPackages;
    }

    private static void readPackages( @Nonnull Map<String, String> properties, @Nonnull String path )
    {
        // extra packages exported by boot delegation & the system bundle
        URL sysPkgResource = SystemPackages.class.getResource( path );
        if( sysPkgResource == null )
        {
            throw bootstrapError( "Could not find system packages file at '%s' - cannot boot server", path );
        }

        Properties tempProperties = new Properties();
        try( InputStream is = sysPkgResource.openStream() )
        {
            tempProperties.load( is );

            List<String> packageNames = new ArrayList<>( tempProperties.stringPropertyNames() );
            Collections.sort( packageNames );
            for( String packageName : packageNames )
            {
                String version = tempProperties.getProperty( packageName );

                Matcher matcher = PROPERTY_PATTERN.matcher( version );
                if( matcher.matches() )
                {
                    String propertyName = matcher.group( 1 );
                    switch( propertyName )
                    {
                        case "javax.annotation.version":
                            // TODO arik: discover annotations version here
                            version = "2.0.1";
                            break;

                        case "log4j.version":
                            // TODO arik: discover log4j version here
                            version = "1.2.17";
                            break;

                        case "slf4j.version":
                            // TODO arik: discover slf4j version here
                            version = "1.7.1";
                            break;

                        case "jcl.version":
                            // TODO arik: discover apache-commons-logging version here
                            version = "1.1.1";
                            break;

                        default:
                            throw bootstrapError( "Unknown version property '%s' under package '%s'", propertyName, packageName );
                    }
                }
                properties.put( packageName, version );
            }
        }
        catch( IOException e )
        {
            throw bootstrapError( "Cannot read system packages from '%s': %s", e, sysPkgResource, e.getMessage() );
        }
    }

    private static String createSpecForPackages( @Nonnull Map<String, String> packages, boolean withVersions )
    {
        StringBuilder buffer = new StringBuilder( 2000 );
        for( Map.Entry<String, String> entry : packages.entrySet() )
        {
            String packageName = entry.getKey().trim();

            String version = entry.getValue();
            if( entry.getValue() == null )
            {
                LOG.warn( "Null version for package '{}' detected", packageName );
                continue;
            }
            version = version.trim();

            if( buffer.length() > 0 )
            {
                buffer.append( "," );
            }

            if( withVersions )
            {
                buffer.append( packageName ).append( ";version=" ).append( version );
            }
            else
            {
                buffer.append( packageName );
            }
        }
        return buffer.toString();
    }
}
