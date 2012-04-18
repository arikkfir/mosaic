package org.mosaic.server.shell.commands.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.joda.time.DateTime;
import org.mosaic.describe.Description;
import org.mosaic.osgi.BundleStatus;
import org.mosaic.osgi.util.BundleUtils;
import org.mosaic.server.shell.Args;
import org.mosaic.server.shell.Option;
import org.mosaic.server.shell.ShellCommand;
import org.mosaic.server.shell.console.Console;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleRevisions;
import org.springframework.stereotype.Component;

import static org.mosaic.osgi.util.BundleUtils.findMatchingBundles;

/**
 * @author arik
 */
@Component
public class InspectBundleCommand extends AbstractCommand {

    @Description( "Inspect given bundle" )
    @ShellCommand( "inspect" )
    public void inspectBundle( Console console,

                               @Option( alias = "e" )
                               @Description( "exact matching (filter arguments will not be treated as wildcards)" )
                               boolean exact,

                               @Option( alias = "s" )
                               @Description( "show registered and used services" )
                               boolean services,

                               @Option( alias = "c" )
                               @Description( "show bundle capabilities" )
                               boolean capabilities,

                               @Option( alias = "r" )
                               @Description( "show bundle requirements" )
                               boolean requirements,

                               @Option( alias = "w" )
                               @Description( "show bundle wires" )
                               boolean wires,

                               @Args
                               String... filters

    ) throws IOException {

        List<Bundle> matches = findMatchingBundles( getBundleContext(), exact, filters );
        if( matches.isEmpty() ) {

            console.println( "No bundles match requested filters." );

        } else {

            for( Bundle bundle : matches ) {
                showBundle( console, bundle, services, capabilities, requirements, wires );
            }

        }
    }

    private void showBundle( Console console,
                             Bundle bundle, boolean services,
                             boolean capabilities,
                             boolean requirements,
                             boolean wires )
            throws IOException {
        //FEATURE: headers (only latest revision - highlight that!)
        BundleStatus status = getBundleStatus( bundle );

        console.println();
        console.println();
        showGeneralInfo( console, bundle, status );

        console.println();
        showRevisions( console, bundle, capabilities, requirements, wires );

        if( services ) {
            showServices( console, bundle );
        }

        console.println();
        console.println();
    }

    private void showGeneralInfo( Console console, Bundle bundle, BundleStatus status ) throws IOException {
        console.println( "General information:" );
        console.println( "====================" );
        console.print( "Bundle ID:     " ).println( bundle.getBundleId() );
        console.print( "Symbolic name: " ).println( bundle.getSymbolicName() );
        console.print( "Version:       " ).println( bundle.getVersion() );
        console.print( "State:         " ).println( status.getState() );
        console.print( "Last modified: " ).println( new DateTime( bundle.getLastModified() ) );
        console.print( "Location:      " ).println( bundle.getLocation() );

        Collection<String> unsatisfiedRequirements = status.getUnsatisfiedRequirements();
        if( unsatisfiedRequirements != null && !unsatisfiedRequirements.isEmpty() ) {
            console.println();
            console.println( "Missing requirements:" );
            console.println( "---------------------" );
            for( String requirement : unsatisfiedRequirements ) {
                console.print( "  " ).println( requirement );
            }
        }
    }

    private void showRevisions( Console console,
                                Bundle bundle,
                                boolean capabilities,
                                boolean requirements,
                                boolean wires ) throws IOException {
        console.println( "Bundle revisions (first is latest):" );
        console.println( "-----------------------------------" );
        boolean first = true;
        for( BundleRevision revision : bundle.adapt( BundleRevisions.class ).getRevisions() ) {
            if( !first ) {
                console.println( "-------------------------------------------------" );
            } else {
                first = false;
            }
            showRevision( console, capabilities, requirements, wires, revision );
        }
    }

    private void showRevision( Console console,
                               boolean capabilities,
                               boolean requirements,
                               boolean wires,
                               BundleRevision revision ) throws IOException {
        console.print( "Symbolic name: " ).println( revision.getSymbolicName() );
        console.print( "Version:       " ).println( revision.getVersion() );
        if( capabilities ) {
            console.println( "Capabilities:  " );
            for( BundleCapability capability : revision.getDeclaredCapabilities( null ) ) {
                console.print( "  " ).println( capability );
            }
        }
        if( requirements ) {
            console.println( "Requirements:  " );
            for( BundleRequirement requirement : revision.getDeclaredRequirements( null ) ) {
                console.print( "  " ).println( requirement );
            }
        }
/*
        if( wires ) {
            BundleWiring wiring = revision.getWiring();
            if( wiring.isInUse() ) {
                console.println( "Wires:         " );
                List<BundleWire> providedWires = wiring.getProvidedWires( null );
                if( providedWires != null ) {
                    for( BundleWire wire : providedWires ) {
                        //FEATURE 4/14/12: show wire
                    }
                }
                List<BundleWire> requiredWires = wiring.getRequiredWires( null );
                if( requiredWires != null ) {
                    for( BundleWire wire : requiredWires ) {
                        //FEATURE 4/14/12: show wire
                    }
                }
            }
        }
*/
    }

    private void showServices( Console console, Bundle bundle ) throws IOException {
        ServiceReference<?>[] providedServices = bundle.getRegisteredServices();
        if( providedServices != null ) {
            console.println();
            console.println( "Provided services:" );
            console.println( "------------------" );
            for( ServiceReference<?> reference : providedServices ) {
                for( String propertyKey : reference.getPropertyKeys() ) {
                    Object value = reference.getProperty( propertyKey );
                    if( value instanceof String[] ) {
                        value = Arrays.asList( ( String[] ) value );
                    }
                    console.print( "  " ).print( propertyKey ).print( ": " ).println( value );
                }
                console.println( "  -----------------------------" );
                Bundle[] usingBundles = reference.getUsingBundles();
                if( usingBundles != null ) {
                    for( Bundle usingBundle : usingBundles ) {
                        console.print( "  Used by: " ).println( BundleUtils.toString( usingBundle ) );
                    }
                } else {
                    console.println( "  Not used by any bundle." );
                }
                console.println();
            }
        }

        ServiceReference<?>[] servicesInUse = bundle.getServicesInUse();
        if( servicesInUse != null ) {
            console.println();
            console.println( "Used services:" );
            console.println( "--------------" );
            for( ServiceReference<?> reference : servicesInUse ) {
                for( String propertyKey : reference.getPropertyKeys() ) {
                    Object value = reference.getProperty( propertyKey );
                    if( value instanceof String[] ) {
                        value = Arrays.asList( ( String[] ) value );
                    }
                    console.print( "  " ).print( propertyKey ).print( ": " ).println( value );
                }
                console.println( "  -----------------------------" );
                Bundle providingBundle = reference.getBundle();
                if( providingBundle != null ) {
                    console.print( "  Provided by: " ).println( BundleUtils.toString( providingBundle ) );
                } else {
                    console.println( "  Service already unregistered." );
                }
                console.println();
            }
        }
    }

}
