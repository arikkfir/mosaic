package org.mosaic.runner.boot;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.framework.Felix;
import org.apache.felix.framework.cache.BundleCache;
import org.apache.felix.framework.util.FelixConstants;
import org.mosaic.runner.ExitCode;
import org.mosaic.runner.ServerHome;
import org.mosaic.runner.SystemExitException;
import org.mosaic.runner.logging.FelixLogger;
import org.mosaic.runner.logging.LoggingBundleListener;
import org.mosaic.runner.logging.LoggingFrameworkListener;
import org.mosaic.runner.logging.LoggingServiceListener;
import org.osgi.framework.Constants;

/**
 * Boots the server.
 *
 * @author arik
 */
public class FrameworkBootstrapper {

    private final ServerHome home;

    public FrameworkBootstrapper( ServerHome home ) {
        this.home = home;
    }

    public Felix boot() throws SystemExitException {
        Felix felix = createFelix();

        try {

            // create boot artifacts resolvers
            Map<String, BootArtifactResolver> resolvers = new HashMap<>();
            resolvers.put( "mvn", new MavenBundleBootArtifactResolver( felix.getBundleContext() ) );
            resolvers.put( "file", new FileBundleBootArtifactResolver( felix.getBundleContext() ) );

            // install all boot jars and links
            File[] bootJars = this.home.getBoot().listFiles();
            if( bootJars != null ) {
                for( File file : bootJars ) {
                    installBootArtifact( resolvers, file );
                }
            }

            // start and return the framework instance
            felix.start();
            return felix;

        } catch( SystemExitException e ) {

            throw e;

        } catch( Exception e ) {

            throw new SystemExitException( "Could not start OSGi container (Apache Felix): " + e.getMessage(), e, ExitCode.START_ERROR );

        }
    }

    private Felix createFelix() throws SystemExitException {
        try {

            // build Felix configuration
            Map<String, Object> felixConfig = new HashMap<>();

            // storage properties
            File felixWorkDir = new File( this.home.getWork(), "felix" );
            if( Boolean.getBoolean( "clean" ) && felixWorkDir.exists() ) {
                FileUtils.forceDelete( felixWorkDir );
            }
            felixConfig.put( FelixConstants.FRAMEWORK_STORAGE, felixWorkDir.toString() );
            felixConfig.put( BundleCache.CACHE_BUFSIZE_PROP, ( 1024 * 64 ) + "" );

            // logging properties
            felixConfig.put( FelixConstants.LOG_LOGGER_PROP, new FelixLogger() );
            felixConfig.put( FelixConstants.LOG_LEVEL_PROP, FelixLogger.LOG_DEBUG + "" );

            // start-level
            felixConfig.put( FelixConstants.FRAMEWORK_BEGINNING_STARTLEVEL, "5" );

            // system packages
            felixConfig.put( Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA,
                             "sun.misc," +
                             "org.slf4j;version=1.6.4," +
                             "org.slf4j.spi;version=1.6.4," +
                             "org.slf4j.helpers;version=1.6.4" );

            // create Felix instance with logging listeners
            Felix felix = new Felix( felixConfig );
            felix.init();
            felix.getBundleContext().addFrameworkListener( new LoggingFrameworkListener() );
            felix.getBundleContext().addBundleListener( new LoggingBundleListener() );
            felix.getBundleContext().addServiceListener( new LoggingServiceListener() );
            return felix;

        } catch( Exception e ) {
            throw new SystemExitException( "Could not create OSGi container (Apache Felix): " + e.getMessage(), e, ExitCode.START_ERROR );
        }
    }

    private void installBootArtifact( Map<String, BootArtifactResolver> resolvers, File file )
            throws SystemExitException {

        String extension = FilenameUtils.getExtension( file.getName() );
        if( extension.equalsIgnoreCase( "jars" ) ) {

            processBootLinks( resolvers, file );

        } else if( extension.equalsIgnoreCase( "jar" ) ) {

            resolvers.get( "file" ).resolve( this.home, new BootArtifact( "file", file.getAbsolutePath() ) );

        }
    }

    private void processBootLinks( Map<String, BootArtifactResolver> resolvers, File file ) throws SystemExitException {
        try {

            for( String line : FileUtils.readLines( file ) ) {
                if( !StringUtils.isBlank( line ) && !line.startsWith( "#" ) ) {
                    processBootLink( resolvers, file, line );
                }
            }

        } catch( IOException e ) {
            throw new SystemExitException( "Cannot read '" + file + "': " + e.getMessage(), e, ExitCode.START_ERROR );
        }
    }

    private void processBootLink( Map<String, BootArtifactResolver> resolvers, File file, String line )
            throws SystemExitException {

        BootArtifact artifact = new BootArtifact( line );
        BootArtifactResolver resolver = resolvers.get( artifact.getType() );
        if( resolver == null ) {
            throw new SystemExitException( "Unknown boot artifact type '" + artifact.getType() + "' in file '" + file + "'", ExitCode.CONFIG_ERROR );
        } else {
            resolver.resolve( this.home, artifact );
        }
    }
}
