package org.mosaic.util.osgi;

import javax.annotation.Nonnull;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/**
 * @author arik
 */
public final class BundleUtils
{
    @Nonnull
    public static BundleContext requireBundleContext( @Nonnull Class<?> type )
    {
        BundleContext bundleContext = FrameworkUtil.getBundle( type ).getBundleContext();
        if( bundleContext == null )
        {
            throw new IllegalStateException( "no bundle context for 'modules' module" );
        }
        else
        {
            return bundleContext;
        }
    }

    private BundleUtils()
    {
    }
}
