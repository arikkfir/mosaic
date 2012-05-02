package org.mosaic.runner.boot.artifact.resolve;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.mosaic.runner.ServerHome;
import org.mosaic.runner.boot.artifact.BootArtifact;
import org.mosaic.runner.boot.artifact.CannotInstallBootArtifactException;
import org.mosaic.runner.util.FileMatcher;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * @author arik
 */
public class FileBundleBootArtifactResolver extends AbstractBootArtifactResolver implements BootArtifactResolver
{
    public FileBundleBootArtifactResolver( BundleContext bundleContext )
    {
        super( bundleContext );
    }

    @Override
    public Set<Bundle> resolve( ServerHome home, BootArtifact artifact ) throws CannotInstallBootArtifactException
    {

        File file = new File( artifact.getCoordinates( ) );
        if( !file.isAbsolute( ) )
        {
            file = new File( home.getHome( ), artifact.getCoordinates( ) );
        }

        Set<Bundle> bundles = new HashSet<>( );
        List<File> matches = FileMatcher.find( file.getAbsolutePath( ) );
        for( File match : matches )
        {
            Set<Bundle> resolvedBundles = installOrUpdateBundle( artifact, match );
            if( resolvedBundles != null )
            {
                bundles.addAll( resolvedBundles );
            }
        }
        return bundles;
    }

}
