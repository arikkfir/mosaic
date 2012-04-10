package org.mosaic.server.shell.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import jline.console.ConsoleReader;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import static org.osgi.framework.Bundle.*;

/**
 * @author arik
 */
public class WelcomeMessage {

    public static void print( BundleContext bundleContext, ConsoleReader consoleReader ) throws IOException {
        consoleReader.println( "" );
        consoleReader.println( "*************************************************************" );
        consoleReader.println( "Welcome to Mosaic Server! (running on " + System.getProperty( "os.name" ) + ")" );
        printBundleCounts( bundleContext, consoleReader );
        consoleReader.println( "" );
        consoleReader.println( "*************************************************************" );
        consoleReader.println( "" );
    }

    private static void printBundleCounts( BundleContext bundleContext, ConsoleReader consoleReader )
            throws IOException {
        Bundle[] bundles = bundleContext.getBundles();
        if( bundles == null ) {
            consoleReader.println();
            consoleReader.println( "No bundles are hosted!" );
            consoleReader.println();
            return;
        }

        Map<Integer, Integer> states = new HashMap<>();
        states.put( INSTALLED, 0 );
        states.put( RESOLVED, 0 );
        states.put( STARTING, 0 );
        states.put( ACTIVE, 0 );
        states.put( STOPPING, 0 );
        states.put( UNINSTALLED, 0 );

        consoleReader.println();
        consoleReader.println( "Server bundles:" );
        consoleReader.println( "---------------" );
        for( Bundle bundle : bundles ) {
            if( bundle.getSymbolicName().startsWith( "org.mosaic" ) ) {
                consoleReader.println( "  " + rightPad( bundle.getSymbolicName(), 25 ) + ": " + bundle.getVersion() );
            }
            states.put( bundle.getState(), states.get( bundle.getState() ) + 1 );
        }

        consoleReader.println();
        consoleReader.println( "Currently hosting:" );
        consoleReader.println( "------------------" );
        consoleReader.println( "  " + leftPad( states.get( INSTALLED ), 3 ) + " installed bundles" );
        consoleReader.println( "  " + leftPad( states.get( RESOLVED ), 3 ) + " resolved bundles" );
        consoleReader.println( "  " + leftPad( states.get( STARTING ), 3 ) + " starting bundles" );
        consoleReader.println( "  " + leftPad( states.get( ACTIVE ), 3 ) + " active bundles (including published bundles)" );
        consoleReader.println( "  " + leftPad( states.get( STOPPING ), 3 ) + " stopping bundles" );
        consoleReader.println( "  " + leftPad( states.get( UNINSTALLED ), 3 ) + " uninstalled bundles" );
    }

    private static String rightPad( String text, int length ) {
        while( text.length() < length ) {
            text += ' ';
        }
        return text;
    }

    private static String leftPad( Object value, int length ) {
        String text = value.toString();
        while( text.length() < length ) {
            text = ' ' + text;
        }
        return text;
    }
}
