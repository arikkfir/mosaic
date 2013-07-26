package org.mosaic.launcher.osgi;

import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import javax.annotation.Nonnull;
import org.apache.felix.framework.Felix;
import org.mosaic.launcher.MosaicInstance;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import static java.nio.file.Files.*;
import static java.nio.file.StandardOpenOption.READ;
import static java.util.Arrays.asList;
import static org.mosaic.launcher.util.SystemError.bootstrapError;

/**
 * @author arik
 */
public class BootBundlesInstaller
{
    private static final String PATH_SEPARATOR = System.getProperty( "path.separator" );

    private static final List<String> BOOT_BUNDLE_NAMES = asList( "api",
                                                                  "lifecycle",
                                                                  "config",
                                                                  "database",
                                                                  "event",
                                                                  "mail",
                                                                  "metrics",
                                                                  "security",
                                                                  "shell",
                                                                  "validation",
                                                                  "web" );

    public BootBundlesInstaller( @Nonnull MosaicInstance mosaic, @Nonnull Felix felix )
    {
        Properties properties = mosaic.getProperties();

        List<Bundle> bundles = new LinkedList<>();
        for( String name : BOOT_BUNDLE_NAMES )
        {
            Path bundlePath = null;

            String location = properties.getProperty( "mosaic.boot." + name );
            if( location != null )
            {
                bundlePath = mosaic.getHome().resolve( location ).normalize().toAbsolutePath();
                verifyInstallableBundle( name, bundlePath );
            }

            String versionedFilename = name + "-" + mosaic.getVersion() + ".jar";
            if( bundlePath == null )
            {
                StringTokenizer tokenizer = new StringTokenizer( ManagementFactory.getRuntimeMXBean().getClassPath(), PATH_SEPARATOR, false );
                while( tokenizer.hasMoreTokens() )
                {
                    String item = tokenizer.nextToken();
                    if( item.contains( "/" + name + "/target/classes" ) )
                    {
                        bundlePath = Paths.get( item ).getParent().resolve( versionedFilename );
                        verifyInstallableBundle( name, bundlePath );
                        break;
                    }
                    else if( item.endsWith( versionedFilename ) )
                    {
                        bundlePath = Paths.get( item );
                        verifyInstallableBundle( name, bundlePath );
                        break;
                    }
                }
            }
            if( bundlePath == null )
            {
                bundlePath = mosaic.getHome().resolve( "boot" ).resolve( versionedFilename ).normalize().toAbsolutePath();
                verifyInstallableBundle( name, bundlePath );
            }

            try
            {
                BundleContext bc = felix.getBundleContext();
                bundles.add( bc.installBundle( bundlePath.toUri().toString(), newInputStream( bundlePath, READ ) ) );
            }
            catch( Exception e )
            {
                throw bootstrapError( "Could not install boot bundle at '{}': {}", bundlePath, e.getMessage(), e );
            }
        }
        for( Bundle bundle : bundles )
        {
            try
            {
                bundle.start();
            }
            catch( Throwable e )
            {
                throw bootstrapError( "Could not start boot bundle at '{}': {}", bundle.getLocation(), e.getMessage(), e );
            }
        }
        System.setProperty( "mosaic.started", "true" );
    }

    public void stop() throws InterruptedException
    {
        System.setProperty( "mosaic.started", "false" );
    }

    private void verifyInstallableBundle( @Nonnull String name, @Nonnull Path file )
    {
        if( !exists( file ) )
        {
            throw bootstrapError( "Could not find bundle '{}' at '{}'", name, file );
        }
        else if( !isRegularFile( file ) )
        {
            throw bootstrapError( "Bundle at '{}' is not a file", file );
        }
        else if( !isReadable( file ) )
        {
            throw bootstrapError( "Bundle at '{}' is not readable", file );
        }
    }
}
