package org.mosaic.launcher;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import org.apache.felix.framework.Felix;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.startlevel.BundleStartLevel;

import static java.io.File.pathSeparator;
import static java.lang.management.ManagementFactory.getRuntimeMXBean;
import static java.nio.file.Files.*;
import static org.mosaic.launcher.SystemError.bootstrapError;

/**
 * @author arik
 */
final class InitBootBundlesTask extends InitTask
{
    private static final Pattern JAR_FILE_PATTERN = Pattern.compile( "(\\p{ASCII}+)\\-\\d+(?:\\.\\d+)*(?:[\\-|\\.]\\p{ASCII}+)?\\.jar" );

    private static final Set<String> BOOT_BUNDLE_NAMES;

    static
    {
        Set<String> bootBundleNames = new LinkedHashSet<>();
        bootBundleNames.add( "org.apache.felix.log" );
        bootBundleNames.add( "org.apache.felix.configadmin" );
        bootBundleNames.add( "org.apache.felix.eventadmin" );
        bootBundleNames.add( "guava" );
        bootBundleNames.add( "jcl-over-slf4j" );
        bootBundleNames.add( "log4j-over-slf4j" );
        bootBundleNames.add( "joda-time" );
        bootBundleNames.add( "classmate" );
        bootBundleNames.add( "jboss-logging" );
        bootBundleNames.add( "validation-api" );
        bootBundleNames.add( "hibernate-validator" );
        bootBundleNames.add( "javax.el-api" );
        bootBundleNames.add( "javax.el" );
        bootBundleNames.add( "org.mosaic.util" );
        bootBundleNames.add( "org.mosaic.modules" );
        BOOT_BUNDLE_NAMES = Collections.unmodifiableSet( bootBundleNames );
    }

    @Nonnull
    private final InitFelixTask felixTask;

    InitBootBundlesTask( @Nonnull Mosaic mosaic, @Nonnull InitFelixTask initFelixTask )
    {
        super( mosaic );
        this.felixTask = initFelixTask;
    }

    @Override
    public void start()
    {
        Felix felix = this.felixTask.getFelix();
        if( felix == null )
        {
            throw bootstrapError( "Felix instance not created yet (this shouldn't happen)" );
        }

        List<Bundle> bootBundles = new LinkedList<>();

        // first just install the boot bundles without starting them (so they can use classes from one another in any order)
        this.log.debug( "Installing boot bundles" );
        for( String bootBundleName : BOOT_BUNDLE_NAMES )
        {
            bootBundles.add( installBootBundle( felix, bootBundleName ) );
        }

        // now that we've installed them, lets start them
        this.log.debug( "Starting boot bundles: {}", bootBundles );
        for( Bundle bundle : bootBundles )
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
    }

    @Override
    public void stop()
    {
        // no-op
    }

    private Bundle installBootBundle( Felix felix, String bootBundleName )
    {
        // search for the boot bundle file in boot dir
        Path file = null;
        try( DirectoryStream<Path> boot = Files.newDirectoryStream( getBoot() ) )
        {
            for( Path path : boot )
            {
                Matcher matcher = JAR_FILE_PATTERN.matcher( path.getFileName().toString() );
                if( matcher.matches() )
                {
                    String foundBundleName = matcher.group( 1 );
                    if( foundBundleName.equalsIgnoreCase( bootBundleName ) )
                    {
                        file = path;
                        break;
                    }
                }
            }

            if( file == null )
            {
                // search for the boot bundle jar in the classpath
                for( String cpToken : getRuntimeMXBean().getClassPath().split( pathSeparator ) )
                {
                    Path cpTokenFile = Paths.get( cpToken );
                    Matcher matcher = JAR_FILE_PATTERN.matcher( cpTokenFile.getFileName().toString() );
                    if( matcher.matches() )
                    {
                        String foundBundleName = matcher.group( 1 );
                        if( foundBundleName.equalsIgnoreCase( bootBundleName ) )
                        {
                            file = cpTokenFile;
                            break;
                        }
                    }
                    else if( "classes".equals( cpTokenFile.getFileName().toString() ) )
                    {
                        Path targetDir = cpTokenFile.getParent();
                        if( "target".equals( targetDir.getFileName().toString() ) )
                        {
                            Path moduleDir = targetDir.getParent();
                            if( bootBundleName.equals( moduleDir.getFileName().toString() ) )
                            {
                                Path bundleFile = targetDir.resolve( bootBundleName + "-" + getVersion() + ".jar" );
                                if( exists( bundleFile ) && isRegularFile( bundleFile ) )
                                {
                                    file = bundleFile;
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            if( file == null )
            {
                throw bootstrapError( "Could not find boot bundle '{}'", bootBundleName );
            }
        }
        catch( IOException e )
        {
            throw bootstrapError( "Could not find boot bundle '{}'", bootBundleName );
        }

        // get bundle context
        BundleContext bc = felix.getBundleContext();
        if( bc == null )
        {
            throw bootstrapError( "Felix bundle context not available" );
        }

        Bundle bundle;
        try
        {
            // install the bundle
            this.log.debug( "Installing boot bundle at '{}'", file );
            bundle = bc.installBundle( file.toString(), newInputStream( file ) );
            bundle.adapt( BundleStartLevel.class ).setStartLevel( 1 );
        }
        catch( Exception e )
        {
            throw bootstrapError( "Could not install boot bundle at '{}': {}", file, e.getMessage(), e );
        }
        return bundle;
    }
}
