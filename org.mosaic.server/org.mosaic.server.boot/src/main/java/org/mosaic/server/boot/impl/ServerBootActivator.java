package org.mosaic.server.boot.impl;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.mosaic.Home;
import org.mosaic.server.boot.impl.logging.LogWeaver;
import org.mosaic.server.boot.impl.publish.spring.OsgiSpringNamespacePlugin;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * @author arik
 */
public class ServerBootActivator implements BundleActivator {

    private MosaicLogListener logListener;

    private OsgiSpringNamespacePlugin springNamespacePlugin;

    private LogWeaver logWeaver;

    private BundleBootstrapper bundleBootstrapper;

    @Override
    public void start( BundleContext bundleContext ) throws Exception {
        bundleContext.registerService( Home.class, new HomeService(), null );

        this.logListener = new MosaicLogListener( bundleContext );
        this.logListener.open();

        this.springNamespacePlugin = new OsgiSpringNamespacePlugin( bundleContext );
        this.springNamespacePlugin.open();

        this.logWeaver = new LogWeaver();
        this.logWeaver.open( bundleContext );

        this.bundleBootstrapper = new BundleBootstrapper( bundleContext, this.springNamespacePlugin );
        this.bundleBootstrapper.open();
    }

    @Override
    public void stop( BundleContext bundleContext ) throws Exception {
        this.bundleBootstrapper.close();
        this.bundleBootstrapper = null;

        this.logWeaver.close();
        this.logWeaver = null;

        this.springNamespacePlugin.close();
        this.springNamespacePlugin = null;

        this.logListener.close();
        this.logListener = null;
    }

    private class HomeService implements Home {

        private final Path home;

        private final Path boot;

        private final Path deploy;

        private final Path etc;

        private final Path server;

        private final Path work;

        private HomeService() {
            this.home = Paths.get( System.getProperty( "mosaic.home" ) );
            this.boot = Paths.get( System.getProperty( "mosaic.home.boot" ) );
            this.deploy = Paths.get( System.getProperty( "mosaic.home.deploy" ) );
            this.etc = Paths.get( System.getProperty( "mosaic.home.etc" ) );
            this.server = Paths.get( System.getProperty( "mosaic.home.server" ) );
            this.work = Paths.get( System.getProperty( "mosaic.home.work" ) );
        }

        @Override
        public Path getHome() {
            return this.home;
        }

        @Override
        public Path getBoot() {
            return this.boot;
        }

        @Override
        public Path getDeploy() {
            return this.deploy;
        }

        @Override
        public Path getEtc() {
            return this.etc;
        }

        @Override
        public Path getServer() {
            return this.server;
        }

        @Override
        public Path getWork() {
            return this.work;
        }
    }
}
