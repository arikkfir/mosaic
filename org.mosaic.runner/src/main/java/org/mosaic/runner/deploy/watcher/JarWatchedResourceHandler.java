package org.mosaic.runner.deploy.watcher;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import static java.nio.file.Files.newInputStream;
import static java.nio.file.StandardOpenOption.READ;

/**
 * @author arik
 */
public class JarWatchedResourceHandler implements WatchedResourceHandler {

    private final BundleContext bundleContext;

    public JarWatchedResourceHandler( BundleContext bundleContext ) {
        this.bundleContext = bundleContext;
    }

    @Override
    public void handleNoLongerExists( Path resource ) throws BundleException {
        Bundle bundle = findBundle( bundleContext, resource );
        if( bundle != null ) {
            bundle.uninstall();
        }
    }

    @Override
    public void handleIllegalFile( Path resource ) throws BundleException {
        Bundle bundle = findBundle( bundleContext, resource );
        if( bundle != null ) {
            bundle.uninstall();
        }
    }

    @Override
    public Long getLastUpdateTime( Path resource ) {
        Bundle bundle = findBundle( this.bundleContext, resource );
        if( bundle == null ) {
            return null;
        } else {
            return bundle.getLastModified();
        }
    }

    @Override
    public void handleUpdated( Path resource ) throws IOException, BundleException {
        Bundle bundle = findBundle( this.bundleContext, resource );
        if( bundle == null || bundle.getState() == Bundle.UNINSTALLED ) {

            //
            // first time we're processing this file - install it
            //
            bundleContext.installBundle( getBundleLocationFromResource( resource ), newInputStream( resource, READ ) );

        } else if( bundle.getState() == Bundle.STARTING ) {

            //
            // if the file represents a bundle in the STARTING phase, we won't be able to manipulate it anyway, so
            // just log it and hope it will be resolved later
            //
            throw new IllegalStateException( "Bundle " + bundle.getBundleId() + " cannot be updated from '" + resource + "' because it is in the STARTING state" );

        } else if( bundle.getState() == Bundle.STOPPING ) {

            //
            // if the file represents a bundle in the STOPPING phase, we won't be able to manipulate it anyway, so
            // just log it and hope it will be resolved later
            //
            throw new IllegalStateException( "Bundle " + bundle.getBundleId() + " cannot be updated from '" + resource + "' because it is in the STOPPING state" );

        } else {

            //
            // a bundle was previously installed from this resource - update it to represent the latest resource contents
            //
            bundle.update( newInputStream( resource, StandardOpenOption.READ ) );

        }
    }

    @Override
    public void handleUpToDate( Path resource ) {
        //no-op
    }

    private static String getBundleLocationFromResource( Path resource ) {
        return resource.toUri().toString();
    }

    private static Bundle findBundle( BundleContext bundleContext, Path resource ) {
        return bundleContext.getBundle( getBundleLocationFromResource( resource ) );
    }
}
