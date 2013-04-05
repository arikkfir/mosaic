package org.mosaic.lifecycle.impl.util;

import javax.annotation.Nonnull;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWiring;

/**
 * @author arik
 */
public abstract class BundleUtils
{
    public static Bundle findBundle( BundleContext bundleContext, String symbolicName )
    {
        for( Bundle bundle : bundleContext.getBundles() )
        {
            String sname = bundle.getSymbolicName();
            if( sname != null && sname.equals( symbolicName ) )
            {
                return bundle;
            }
        }
        return null;
    }

    public static String toString( Bundle bundle )
    {
        if( bundle == null )
        {
            return "";
        }
        else
        {
            return bundle.getSymbolicName() + "-" + bundle.getVersion() + "[" + bundle.getBundleId() + "]";
        }
    }

    public static String toString( BundleWiring wiring )
    {
        if( wiring == null )
        {
            return "";
        }
        else
        {
            return toString( wiring.getRevision() );
        }
    }

    public static String toString( BundleRevision revision )
    {
        if( revision == null )
        {
            return "";
        }
        else
        {
            return revision.getSymbolicName() + "-" + revision.getVersion() + "[" + revision + "]";
        }
    }

    public static String toString( BundleContext bundleContext )
    {
        if( bundleContext != null )
        {
            try
            {
                return toString( bundleContext.getBundle() );
            }
            catch( IllegalStateException ignore )
            {
            }
        }
        return "";
    }

    @Nonnull
    public static ClassLoader getClassLoader( @Nonnull BundleContext bundleContext )
    {
        Bundle bundle = bundleContext.getBundle();
        if( bundle.getState() != Bundle.STARTING && bundle.getState() != Bundle.ACTIVE )
        {
            throw new IllegalStateException( "Cannot get class-loader for bundle '" + toString( bundleContext ) + "': not starting or active" );
        }

        BundleWiring bundleWiring = bundle.adapt( BundleWiring.class );
        if( bundleWiring == null )
        {
            throw new IllegalStateException( "Bundle '" + toString( bundleContext ) + "' cannot be adapted to bundle wiring" );
        }

        ClassLoader classLoader = bundleWiring.getClassLoader();
        if( classLoader == null )
        {
            throw new IllegalStateException( "Bundle wiring for '" + toString( bundleContext ) + "' has no class-loader" );
        }
        return classLoader;
    }
}
