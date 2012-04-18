package org.mosaic.server.shell.impl.session;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.mosaic.osgi.util.BundleUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import static org.mosaic.server.shell.impl.util.StringUtils.leftPad;
import static org.mosaic.server.shell.impl.util.StringUtils.rightPad;
import static org.osgi.framework.Bundle.*;

/**
 * @author arik
 */
public class WelcomeMessage {

    public static void print( BundleContext bundleContext, ConsoleImpl console ) throws IOException {
        console.println()
               .println( "*************************************************************" )
               .println()
               .println( "Welcome to Mosaic Server! (running on " + System.getProperty( "os.name" ) + ")" )
               .println( "-------------------------------------------------------------" );

        printBundleCounts( bundleContext, console );

        console.println()
               .println( "*************************************************************" )
               .println();
    }

    private static void printBundleCounts( BundleContext bundleContext, ConsoleImpl console )
            throws IOException {

        Collection<Bundle> bundles = BundleUtils.getAllBundles( bundleContext );
        if( bundles.isEmpty() ) {
            console.println()
                   .println( "No bundles are hosted!" )
                   .println();
            return;
        }

        Map<Integer, Integer> states = new HashMap<>();
        states.put( INSTALLED, 0 );
        states.put( RESOLVED, 0 );
        states.put( STARTING, 0 );
        states.put( ACTIVE, 0 );
        states.put( STOPPING, 0 );
        states.put( UNINSTALLED, 0 );

        console.println()
               .println( "Server bundles:" )
               .println( "---------------" );
        for( Bundle bundle : bundles ) {
            if( bundle.getSymbolicName().startsWith( "org.mosaic" ) ) {
                String symbolicName = rightPad( bundle.getSymbolicName(), 35 );
                String version = bundle.getVersion().toString();
                console.print( "  " ).print( symbolicName ).print( ": " ).println( version );
            }
            states.put( bundle.getState(), states.get( bundle.getState() ) + 1 );
        }

        console.println()
               .println( "Currently hosting:" )
               .println( "------------------" )
               .print( "  " ).print( leftPad( states.get( INSTALLED ), 3 ) ).println( " installed bundles" )
               .print( "  " ).print( leftPad( states.get( RESOLVED ), 3 ) ).println( " resolved bundles" )
               .print( "  " ).print( leftPad( states.get( STARTING ), 3 ) ).println( " starting bundles" )
               .print( "  " ).print( leftPad( states.get( ACTIVE ), 3 ) ).println( " active bundles (including published bundles)" )
               .print( "  " ).print( leftPad( states.get( STOPPING ), 3 ) ).println( " stopping bundles" )
               .print( "  " ).print( leftPad( states.get( UNINSTALLED ), 3 ) ).println( " uninstalled bundles" );
    }
}
