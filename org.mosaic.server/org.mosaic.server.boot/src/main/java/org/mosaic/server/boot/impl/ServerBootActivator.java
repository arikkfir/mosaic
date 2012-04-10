package org.mosaic.server.boot.impl;

import org.mosaic.MosaicHome;
import org.mosaic.server.boot.impl.publish.spring.OsgiSpringNamespacePlugin;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * @author arik
 */
public class ServerBootActivator implements BundleActivator {

    private MosaicLogListener logListener;

    private OsgiSpringNamespacePlugin springNamespacePlugin;

    private BundleBootstrapper bundleBootstrapper;

    @Override
    public void start( BundleContext bundleContext ) throws Exception {
        bundleContext.registerService( MosaicHome.class, new MosaicHomeService(), null );

        this.logListener = new MosaicLogListener( bundleContext );
        this.logListener.open();

        this.springNamespacePlugin = new OsgiSpringNamespacePlugin( bundleContext );
        this.springNamespacePlugin.open();

        this.bundleBootstrapper = new BundleBootstrapper( bundleContext, this.springNamespacePlugin );
        this.bundleBootstrapper.open();
    }

    @Override
    public void stop( BundleContext bundleContext ) throws Exception {
        this.bundleBootstrapper.close();
        this.bundleBootstrapper = null;

        this.springNamespacePlugin.close();
        this.springNamespacePlugin = null;

        this.logListener.close();
        this.logListener = null;
    }
}
