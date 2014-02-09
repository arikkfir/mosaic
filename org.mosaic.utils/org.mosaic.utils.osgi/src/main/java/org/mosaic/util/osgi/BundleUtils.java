package org.mosaic.util.osgi;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/**
 * OSGi {@link org.osgi.framework.Bundle} utilities.
 *
 * @author arik
 */
public final class BundleUtils
{
    /**
     * Returns the {@link org.osgi.framework.BundleContext bundle context} of the {@link Bundle bundle} that loaded the
     * given class.
     *
     * @param type the class to infer the bundle context from
     * @return OSGi bundle context
     * @throws java.lang.IllegalStateException if the given class was not loaded from an OSGi bundle, or if the bundle from which the given class was loaded from no longer has a bundle context
     */
    @Nullable
    public static BundleContext bundleContext( @Nonnull Class<?> type )
    {
        Bundle bundle = FrameworkUtil.getBundle( type );
        if( bundle == null )
        {
            return null;
        }

        BundleContext bundleContext = bundle.getBundleContext();
        if( bundleContext == null )
        {
            return null;
        }

        return bundleContext;
    }

    /**
     * Returns a beautified string representation of the given {@link org.osgi.framework.BundleContext bundle context}.
     * <p/>
     * The string will contain the symbolic name, version and bundle ID, in the form of:
     * <pre>myBundleSymbolicName@version[id]</pre>
     *
     * @param bundleContext the bundle context
     * @return string
     */
    @Nonnull
    public static String toString( @Nonnull BundleContext bundleContext )
    {
        return toString( bundleContext.getBundle() );
    }

    /**
     * Returns a beautified string representation of the given {@link org.osgi.framework.Bundle bundle}.
     * <p/>
     * The string will contain the symbolic name, version and bundle ID, in the form of:
     * <pre>myBundleSymbolicName@version[id]</pre>
     *
     * @param bundle the bundle
     * @return string
     */
    @Nonnull
    public static String toString( @Nonnull Bundle bundle )
    {
        return bundle.getSymbolicName() + "@" + bundle.getVersion() + "[" + bundle.getBundleId() + "]";
    }

    private BundleUtils()
    {
    }
}
