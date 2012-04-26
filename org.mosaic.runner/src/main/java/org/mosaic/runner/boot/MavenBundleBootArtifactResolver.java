package org.mosaic.runner.boot;

import java.io.File;
import org.mosaic.runner.ServerHome;
import org.osgi.framework.BundleContext;

/**
 * @author arik
 */
public class MavenBundleBootArtifactResolver extends AbstractBootArtifactResolver implements BootArtifactResolver {

    private final File localMavenRepository;

    public MavenBundleBootArtifactResolver( BundleContext bundleContext ) {
        super( bundleContext );
        this.localMavenRepository = new File( System.getProperty( "user.home" ), ".m2/repository" );
    }

    @Override
    public void resolve( ServerHome home, BootArtifact artifact )
            throws CannotInstallBootArtifactException {

        if( !this.localMavenRepository.exists() || !this.localMavenRepository.isDirectory() ) {
            throw new CannotInstallBootArtifactException( artifact, "local maven repository at '" + this.localMavenRepository + "' does not exist or is not a directory" );
        }

        String[] tokens = artifact.getCoordinates().split( ":" );
        if( tokens.length != 3 ) {
            throw new CannotInstallBootArtifactException( artifact, "illegal Maven boot artifact coordinates" );
        }

        File groupDir = new File( this.localMavenRepository, tokens[ 0 ].replace( '.', '/' ) );
        if( !groupDir.exists() || !groupDir.isDirectory() ) {
            throw new CannotInstallBootArtifactException( artifact, "group-ID directory does not exist or is not a directory" );
        }

        File artifactDir = new File( groupDir, tokens[ 1 ] );
        if( !artifactDir.exists() || !artifactDir.isDirectory() ) {
            throw new CannotInstallBootArtifactException( artifact, "artifact-ID directory does not exist or is not a directory" );
        }

        File versionDir = new File( artifactDir, tokens[ 2 ] );
        if( !versionDir.exists() || !versionDir.isDirectory() ) {
            throw new CannotInstallBootArtifactException( artifact, "version directory does not exist or is not a directory" );
        }

        File artifactFile = new File( versionDir, tokens[ 1 ] + "-" + tokens[ 2 ] + ".jar" );
        if( !artifactFile.exists() || !artifactFile.isFile() || !artifactFile.canRead() ) {
            throw new CannotInstallBootArtifactException( artifact, "artifact file does not exist, not a file, or cannot be read" );
        }

        installOrUpdateBundle( artifact, artifactFile );
    }
}
