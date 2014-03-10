package org.mosaic.launcher;

import com.google.common.base.Optional;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.osgi.framework.*;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.parseInt;
import static java.nio.file.FileVisitOption.FOLLOW_LINKS;
import static java.nio.file.Files.*;
import static java.util.Arrays.asList;

/**
 * @author arik
 */
final class BundleScanner implements Runnable
{
    private static final Logger LOG = LoggerFactory.getLogger( BundleScanner.class );

    @Nonnull
    private static final Map<String, Integer> BOOT_BUNDLES;

    static
    {
        Map<String, Integer> bootBundles = new HashMap<>();
        bootBundles.put( "com.fasterxml.classmate", 1 );
        bootBundles.put( "com.fasterxml.jackson.core.jackson-annotations", 1 );
        bootBundles.put( "com.fasterxml.jackson.core.jackson-core", 1 );
        bootBundles.put( "com.fasterxml.jackson.core.jackson-databind", 1 );
        bootBundles.put( "com.fasterxml.jackson.dataformat.jackson-dataformat-csv", 1 );
        bootBundles.put( "com.google.guava", 1 );
        bootBundles.put( "javax.el-api", 1 );
        bootBundles.put( "jcl.over.slf4j", 1 );
        bootBundles.put( "joda-time", 1 );
        bootBundles.put( "log4j.over.slf4j", 1 );
        bootBundles.put( "org.apache.commons.lang3", 1 );
        bootBundles.put( "org.apache.felix.configadmin", 1 );
        bootBundles.put( "org.apache.felix.eventadmin", 1 );
        bootBundles.put( "org.apache.felix.log", 1 );
        bootBundles.put( "org.glassfish.web.javax.el", 1 );
        BOOT_BUNDLES = bootBundles;
    }

    @Nonnull
    private final PathMatcher jarsPathMatcher = FileSystems.getDefault().getPathMatcher( "glob:**/*.jar" );

    @Nonnull
    private final BundleContext bundleContext;

    private boolean running;

    BundleScanner( @Nonnull BundleContext bundleContext )
    {
        this.bundleContext = bundleContext;
    }

    @Override
    public void run()
    {
        // this scanner is not re-entrant; if we're already running on another thread, return
        synchronized( this )
        {
            if( this.running )
            {
                return;
            }
            this.running = true;
        }

        // traverse our bundles directory
        final Context context = new Context();
        try
        {
            walkFileTree( Mosaic.getLib(), EnumSet.of( FOLLOW_LINKS ), MAX_VALUE, new SimpleFileVisitor<Path>()
            {
                @Nonnull
                @Override
                public FileVisitResult visitFile( @Nonnull Path file, @Nonnull BasicFileAttributes attrs )
                        throws IOException
                {
                    // only process *.jar files
                    if( BundleScanner.this.jarsPathMatcher.matches( file ) )
                    {
                        if( isSymbolicLink( file ) )
                        {
                            if( exists( file ) )
                            {
                                context.handleFile( file );
                            }
                            else
                            {
                                handleFileError( "Link '{}' is invalid", file );
                            }
                        }
                        else
                        {
                            context.handleFile( file );
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            } );
            context.process();
        }
        catch( Throwable e )
        {
            LOG.error( "Error while scanning Mosaic server lib directory: {}", e.getMessage(), e );
        }
        finally
        {
            this.running = false;
        }
    }

    @Nonnull
    private String getLocationFromFile( @Nonnull Path file )
    {
        Path absolutePath = file.normalize().toAbsolutePath();
        return "file:" + absolutePath;
    }

    @Nullable
    private Integer getAppropriateStartLevel( Bundle bundle )
    {
        String explicitStartLevel = bundle.getHeaders().get( "Start-Level" );
        if( explicitStartLevel != null )
        {
            try
            {
                return parseInt( explicitStartLevel );
            }
            catch( NumberFormatException ignore )
            {
            }
        }

        if( BOOT_BUNDLES.containsKey( bundle.getSymbolicName() ) )
        {
            return BOOT_BUNDLES.get( bundle.getSymbolicName() );
        }

        return 5;
    }

    private void handleFileError( @Nonnull String message, @Nullable Object... args )
    {
        LOG.error( message, args );
    }

    private class Context
    {
        @Nonnull
        private Optional<List<Path>> locationsToInstall = Optional.absent();

        @Nonnull
        private Optional<List<Path>> locationsToUpdate = Optional.absent();

        @Nonnull
        private Optional<List<Bundle>> bundlesToStart = Optional.absent();

        private void handleFile( @Nonnull Path file )
        {
            Bundle bundle = bundleContext.getBundle( getLocationFromFile( file ) );
            if( bundle == null )
            {
                this.locationsToInstall = addToList( this.locationsToInstall, file );
            }
            else
            {
                try
                {
                    // get file modification time, so we can compare it with the bundle's modification time
                    // we'll ignore files modified in the last second (probably still being modified)
                    long fileModTime = Files.getLastModifiedTime( file ).toMillis();
                    if( fileModTime <= System.currentTimeMillis() - 1000 )
                    {
                        // only update the corresponding bundle if it is NOT up-to-date
                        if( fileModTime > bundle.getLastModified() )
                        {
                            this.locationsToUpdate = addToList( this.locationsToUpdate, file );
                        }
                    }
                }
                catch( IOException e )
                {
                    handleFileError( "Could not extract file modification time of '{}': {}", file, e.getMessage(), e );
                }
            }
        }

        private void process()
        {
            // uninstall bundles whose location files no longer exist
            uninstallBundles();

            // install new bundles
            installBundles();

            // update bundles
            updateBundles();

            // start bundles
            startBundles();
        }

        private void uninstallBundles()
        {
            for( Bundle bundle : bundleContext.getBundles() )
            {
                String location = bundle.getLocation();
                if( location.startsWith( "file:" ) )
                {
                    Path file = Paths.get( location.substring( "file:".length() ) );
                    if( notExists( file ) )
                    {
                        try
                        {
                            bundle.uninstall();
                        }
                        catch( BundleException e )
                        {
                            handleFileError( "Could not uninstall bundle from '{}': {}", file, e.getMessage(), e );
                        }
                    }
                }
            }
        }

        private void installBundles()
        {
            if( this.locationsToInstall.isPresent() )
            {
                for( Path file : this.locationsToInstall.get() )
                {
                    try
                    {
                        Bundle bundle = bundleContext.installBundle( getLocationFromFile( file ) );
                        Integer startlevel = getAppropriateStartLevel( bundle );
                        if( startlevel != null )
                        {
                            bundle.adapt( BundleStartLevel.class ).setStartLevel( startlevel );
                        }
                        this.bundlesToStart = addToList( this.bundlesToStart, bundle );
                    }
                    catch( BundleException e )
                    {
                        handleFileError( "Could not install bundle from '{}': {}", file, e.getMessage(), e );
                    }
                }
            }
        }

        private void updateBundles()
        {
            if( this.locationsToUpdate.isPresent() )
            {
                for( Path file : this.locationsToUpdate.get() )
                {
                    Bundle bundle = bundleContext.getBundle( getLocationFromFile( file ) );
                    if( bundle != null )
                    {
                        // remember if bundle was active, so we'll know whether to start it if update is successful
                        boolean wasActive = bundle.getState() == Bundle.ACTIVE;

                        // stop bundles that depend on this bundle, but also collect them
                        // to our list of bundles to be  started, if they are active now
                        stopAndCollectDependents( bundle );

                        // now stop the bundle
                        try
                        {
                            bundle.stop( Bundle.STOP_TRANSIENT );
                        }
                        catch( Exception e )
                        {
                            LOG.error( "Error stopping module at '{}' (stopping in order to update it): {}", file, e.getMessage(), e );
                            continue;
                        }

                        // now update the bundle
                        try
                        {
                            bundle.update();
                        }
                        catch( Exception e )
                        {
                            LOG.error( "Error updating module at '{}': {}", file, e.getMessage(), e );
                            continue;
                        }

                        // refresh packages
                        refreshPackages( bundle );

                        // update successful - if our bundle was active to begin with, remember to start it later
                        if( wasActive )
                        {
                            this.bundlesToStart = addToList( this.bundlesToStart, bundle );
                        }
                    }
                }
            }
        }

        private void startBundles()
        {
            if( this.bundlesToStart.isPresent() )
            {
                for( Bundle bundle : this.bundlesToStart.get() )
                {
                    try
                    {
                        bundle.start();
                    }
                    catch( BundleException e )
                    {
                        LOG.warn( "Error starting bundle '{}-{}[{}]': {}", bundle.getSymbolicName(), bundle.getVersion(), bundle.getBundleId(), e.getMessage(), e );
                    }
                }
            }
        }

        private void refreshPackages( @Nonnull Bundle bundle )
        {
            FrameworkWiring frameworkWiring = bundleContext.getBundle( 0 ).adapt( FrameworkWiring.class );
            if( frameworkWiring == null )
            {
                LOG.warn( "Could not find framework wiring service! This should not be happening..." );
                return;
            }

            final AtomicBoolean refreshComplete = new AtomicBoolean( false );
            frameworkWiring.refreshBundles( asList( bundle ), new FrameworkListener()
            {
                @Override
                public void frameworkEvent( FrameworkEvent event )
                {
                    refreshComplete.set( true );
                }
            } );
            while( !refreshComplete.get() )
            {
                try
                {
                    Thread.sleep( 10 );
                }
                catch( InterruptedException e )
                {
                    LOG.error( "Bundle update process of '{}' was interrupted", bundle.getLocation() );
                    return;
                }
            }
        }

        private void stopAndCollectDependents( @Nonnull Bundle bundle )
        {
            BundleWiring bundleWiring = bundle.adapt( BundleWiring.class );
            if( bundleWiring != null )
            {
                List<BundleWire> packageWires = bundleWiring.getProvidedWires( PackageNamespace.PACKAGE_NAMESPACE );
                if( packageWires != null )
                {
                    for( BundleWire packageWire : packageWires )
                    {
                        Bundle requirer = packageWire.getRequirer().getBundle();
                        if( requirer.getState() == Bundle.ACTIVE )
                        {
                            try
                            {
                                requirer.stop( Bundle.STOP_TRANSIENT );
                                this.bundlesToStart = addToList( this.bundlesToStart, requirer );
                            }
                            catch( BundleException e )
                            {
                                LOG.error( "Error stopping module {}-{}[{}] (stopping because depends on {}-{}[{}]): {}",
                                           requirer.getSymbolicName(), requirer.getVersion(), requirer.getBundleId(),
                                           bundle.getSymbolicName(), bundle.getVersion(), bundle.getBundleId(),
                                           e.getMessage(), e );
                            }
                        }
                    }
                }
            }
        }

        @Nonnull
        private <T> Optional<List<T>> addToList( @Nonnull Optional<List<T>> listHolder, @Nonnull T item )
        {
            if( !listHolder.isPresent() )
            {
                List<T> list = new LinkedList<>();
                listHolder = Optional.of( list );
            }
            listHolder.get().add( item );
            return listHolder;
        }
    }
}
