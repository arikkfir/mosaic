package org.mosaic.runner.util;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * @author arik
 */
public abstract class BundleUtils
{
    public static String toString( Bundle bundle )
    {
        if( bundle == null )
        {
            return "";
        }
        else
        {
            return bundle.getSymbolicName( ) + "-" + bundle.getVersion( ) + "[" + bundle.getBundleId( ) + "]";
        }
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public static String toString( BundleContext bundleContext )
    {
        if( bundleContext != null )
        {
            try
            {
                return toString( bundleContext.getBundle( ) );
            }
            catch( IllegalStateException ignore ) {}
        }
        return "";
    }

    public static Collection<Bundle> findBundlesBySymbolicName( BundleContext bundleContext, String symbolicName )
    {
        Bundle[] bundles = bundleContext.getBundles( );
        if( bundles == null || bundles.length == 0 )
        {
            return Collections.emptyList( );
        }

        Collection<Bundle> matches = new LinkedList<>( );
        for( Bundle bundle : bundles )
        {
            if( bundle.getSymbolicName( ).equals( symbolicName ) )
            {
                matches.add( bundle );
            }
        }
        return matches;
    }
}
