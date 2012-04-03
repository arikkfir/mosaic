package org.mosaic.server.boot.impl.track;

import java.io.IOException;
import java.net.URL;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * @author arik
 */
public class OsgiResourcePatternResolver extends PathMatchingResourcePatternResolver {

    private final BundleContext bundleContext;

    public OsgiResourcePatternResolver( BundleContext bundleContext, ClassLoader classLoader ) {
        super( classLoader );
        this.bundleContext = bundleContext;
    }

    @Override
    protected boolean isJarResource( Resource resource ) throws IOException {
        return resource.getURL().getProtocol().equals( "bundle" ) || super.isJarResource( resource );
    }

    @Override
    protected Resource resolveRootDirResource( Resource original ) throws IOException {
        URL originalUrl = original.getURL();
        String protocol = originalUrl.getProtocol();
        if( "bundle".equals( protocol ) ) {
            String bundleIdAndRevision = originalUrl.getHost();
            long bundleId = Long.parseLong( bundleIdAndRevision.substring( 0, bundleIdAndRevision.indexOf( '.' ) ) );
            Bundle bundle = this.bundleContext.getBundle( bundleId );
            if( bundle == null ) {
                return original;
            } else {
                String location = bundle.getLocation();
                URL locationUrl = new URL( location );
                return new UrlResource( "jar:" + locationUrl + "!/" );
            }
        }
        return super.resolveRootDirResource( original );    //To change body of overridden methods use File | Settings | File Templates.
    }
}
