package org.mosaic.launcher.osgi;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import org.apache.felix.framework.Felix;
import org.apache.felix.framework.cache.BundleCache;
import org.apache.felix.framework.util.FelixConstants;
import org.mosaic.launcher.MosaicInstance;
import org.osgi.framework.BundleException;

import static org.mosaic.launcher.util.SystemPackages.getExtraSystemPackages;

/**
 * @author arik
 */
@SuppressWarnings("unchecked")
public class MosaicFelix extends Felix
{
    private static final Integer FELIX_CACHE_BUFSIZE = 1024 * 64;

    private static Map<Object, Object> createFelixConfig( @Nonnull MosaicInstance mosaic )
    {
        Path felixWork = mosaic.getWork().resolve( "felix" );
        Map<Object, Object> felixConfig = new HashMap<>( mosaic.getProperties() );
        felixConfig.put( FelixConstants.FRAMEWORK_STORAGE, felixWork.toString() );                      // specify work location for felix
        felixConfig.put( BundleCache.CACHE_BUFSIZE_PROP, FELIX_CACHE_BUFSIZE.toString() );              // buffer size for reading from storage
        felixConfig.put( FelixConstants.LOG_LEVEL_PROP, "0" );                                          // disable Felix logging output (we'll only log OSGi events)
        felixConfig.put( FelixConstants.FRAMEWORK_BEGINNING_STARTLEVEL, "1" );                          // the framework should start at start-level 1
        felixConfig.put( FelixConstants.BUNDLE_STARTLEVEL_PROP, "1" );                                  // boot bundles should start at start-level 1 as well (will be modified later for app bundles)
        felixConfig.put( FelixConstants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, getExtraSystemPackages() );     // extra packages exported by system bundle
        return felixConfig;
    }

    public MosaicFelix( @Nonnull MosaicInstance mosaic )
    {
        super( createFelixConfig( mosaic ) );
    }

    @Override
    public void start() throws BundleException
    {
        super.start();

        // add a framework listener which logs framework lifecycle events
        getBundleContext().addFrameworkListener( new OsgiEventsLoggingListener() );
        getBundleContext().addServiceListener( new OsgiEventsLoggingListener() );
    }
}
