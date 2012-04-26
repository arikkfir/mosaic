package org.mosaic.runner.boot;

import java.io.File;
import java.util.List;
import org.mosaic.runner.ServerHome;
import org.mosaic.runner.util.FileMatcher;
import org.osgi.framework.BundleContext;

/**
 * @author arik
 */
public class FileBundleBootArtifactResolver extends AbstractBootArtifactResolver implements BootArtifactResolver {

    public FileBundleBootArtifactResolver( BundleContext bundleContext ) {
        super( bundleContext );
    }

    @Override
    public void resolve( ServerHome home, BootArtifact artifact )
            throws CannotInstallBootArtifactException {

        File file = new File( artifact.getCoordinates() );
        if( !file.isAbsolute() ) {
            file = new File( home.getHome(), artifact.getCoordinates() );
        }

        List<File> matches = FileMatcher.find( file.getAbsolutePath() );
        for( File match : matches ) {
            installOrUpdateBundle( artifact, match );
        }
    }

}
