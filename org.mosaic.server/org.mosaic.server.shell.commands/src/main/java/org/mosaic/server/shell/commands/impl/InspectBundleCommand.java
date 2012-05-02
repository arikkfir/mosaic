package org.mosaic.server.shell.commands.impl;

import java.io.IOException;
import java.util.*;
import org.mosaic.describe.Description;
import org.mosaic.server.osgi.BundleStatus;
import org.mosaic.server.osgi.util.BundleUtils;
import org.mosaic.server.shell.Args;
import org.mosaic.server.shell.Option;
import org.mosaic.server.shell.ShellCommand;
import org.mosaic.server.shell.console.Console;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.apache.commons.lang.StringUtils.repeat;
import static org.mosaic.server.osgi.util.BundleUtils.findMatchingBundles;
import static org.osgi.framework.Constants.FILTER_DIRECTIVE;
import static org.osgi.framework.Constants.VERSION_ATTRIBUTE;
import static org.osgi.framework.wiring.BundleRevision.PACKAGE_NAMESPACE;

/**
 * @author arik
 */
@Component
public class InspectBundleCommand extends AbstractCommand
{

    private InspectionListenerHook listenerHook;

    @Autowired
    public void setListenerHook( InspectionListenerHook listenerHook )
    {
        this.listenerHook = listenerHook;
    }

    @Description( "Inspect given bundle" )
    @ShellCommand( "inspect" )
    public void inspectBundle( Console console,

                               @Option( alias = "e" ) @Description( "exact matching (filter arguments will not be treated as wildcards)" ) boolean exact,

                               @Option( alias = "h" ) @Description( "show bundle headers" ) boolean headers,

                               @Option( alias = "s" ) @Description( "show registered and used services" ) boolean services,

                               @Option( alias = "w" ) @Description( "show bundle wires" ) boolean wires,

                               @Option( alias = "p" ) @Description( "show package imports/exports" ) boolean packages,

                               @Args String... filters

    ) throws IOException
    {

        List<Bundle> matches = findMatchingBundles( getBundleContext( ), exact, filters );
        if( matches.isEmpty( ) )
        {

            console.println( "No bundles match requested filters." );

        }
        else
        {

            boolean first = true;
            for( Bundle bundle : matches )
            {
                BundleStatus bundleStatus = getBundleStatus( bundle );

                if( !first )
                {
                    console.println( repeat( "*", console.getWidth( ) - 1 ) );
                }
                else
                {
                    console.println( );
                    first = false;
                }

                showGeneralInfo( console, bundle, bundleStatus );

                if( headers )
                {
                    showHeaders( console, bundle );
                }

                if( services )
                {
                    showServices( console, bundle );
                }

                if( wires )
                {
                    showRequirements( console, bundle );
                    showCapabilities( console, bundle );
                }

                if( packages )
                {
                    showImportedPackages( console, bundle );
                    showProvidedPackages( console, bundle );
                }

                showMissingRequirements( console, bundleStatus );
            }

        }
    }

    private void showGeneralInfo( Console console, Bundle bundle, BundleStatus status ) throws IOException
    {
        console.println( "GENERAL INFORMATION" );
        console.print( "        Bundle ID:     " ).println( bundle.getBundleId( ) );
        console.print( "        Symbolic name: " ).println( bundle.getSymbolicName( ) );
        console.print( "        Version:       " ).println( bundle.getVersion( ) );
        console.print( "        State:         " ).println( status.getState( ) );
        console.print( "        Last modified: " ).println( new Date( bundle.getLastModified( ) ) );
        console.print( "        Location:      " ).println( bundle.getLocation( ) );
    }

    private void showHeaders( Console console, Bundle bundle ) throws IOException
    {
        console.println( "HEADERS" );
        Dictionary<String, String> bundleHeaders = bundle.getHeaders( );
        Enumeration<String> keys = bundleHeaders.keys( );
        while( keys.hasMoreElements( ) )
        {
            String header = keys.nextElement( );
            String value = bundleHeaders.get( header );
            console.print( "        " ).print( header ).print( ": " ).println( value );
        }
    }

    private void showServices( Console console, Bundle bundle ) throws IOException
    {
        ServiceReference<?>[] providedServices = bundle.getRegisteredServices( );
        if( providedServices != null )
        {
            console.println( "REGISTERED SERVICES" );
            boolean first = true;
            for( ServiceReference<?> reference : providedServices )
            {
                if( first )
                {
                    first = false;
                }
                else
                {
                    console.println( "        ---------------------------------------------------------------------------------" );
                }
                for( String propertyKey : reference.getPropertyKeys( ) )
                {
                    Object value = reference.getProperty( propertyKey );
                    if( value instanceof String[] )
                    {
                        value = Arrays.asList( ( String[] ) value );
                    }
                    console.print( "        " ).print( propertyKey ).print( ": " ).println( value );
                }
                Bundle[] usingBundles = reference.getUsingBundles( );
                if( usingBundles != null )
                {
                    for( Bundle usingBundle : usingBundles )
                    {
                        console.print( "        Used by: " ).println( BundleUtils.toString( usingBundle ) );
                    }
                }
                else
                {
                    console.println( "        Not used by any bundle." );
                }
            }
        }

        ServiceReference<?>[] servicesInUse = bundle.getServicesInUse( );
        if( servicesInUse != null )
        {
            console.println( "IMPORTED SERVICES" );
            for( ServiceReference<?> reference : servicesInUse )
            {
                for( String propertyKey : reference.getPropertyKeys( ) )
                {
                    Object value = reference.getProperty( propertyKey );
                    if( value instanceof String[] )
                    {
                        value = Arrays.asList( ( String[] ) value );
                    }
                    console.print( "        " ).print( propertyKey ).print( ": " ).println( value );
                }
                Bundle providingBundle = reference.getBundle( );
                if( providingBundle != null )
                {
                    console.print( "        Provided by: " ).println( BundleUtils.toString( providingBundle ) );
                }
                else
                {
                    console.println( "        Service already unregistered." );
                }
            }
        }

        List<String> requirements = this.listenerHook.getServiceRequirements( bundle );
        if( requirements != null )
        {
            console.println( "LISTENS FOR SERVICES" );
            for( String requirement : requirements )
            {
                console.print( "        " ).println( requirement );
            }
        }
    }

    private void showProvidedPackages( Console console, Bundle bundle ) throws IOException
    {
        BundleRevision revision = bundle.adapt( BundleRevision.class );
        BundleWiring wiring = bundle.adapt( BundleWiring.class );

        Map<BundleCapability, Collection<BundleWire>> allPackageWires = new HashMap<>( 30 );

        List<BundleCapability> packageCapabilities = revision.getDeclaredCapabilities( PACKAGE_NAMESPACE );
        if( packageCapabilities != null )
        {
            for( BundleCapability capability : packageCapabilities )
            {
                allPackageWires.put( capability, new LinkedList<BundleWire>( ) );
            }
        }

        List<BundleWire> providedPackageWires = wiring.getProvidedWires( PACKAGE_NAMESPACE );
        for( BundleWire wire : providedPackageWires )
        {
            BundleCapability capability = wire.getCapability( );
            allPackageWires.get( capability ).add( wire );
        }

        if( allPackageWires.isEmpty( ) )
        {
            console.println( "NO PACKAGE EXPORTS" );
        }
        else
        {
            console.println( "PACKAGE EXPORTS" );
            for( Map.Entry<BundleCapability, Collection<BundleWire>> entry : allPackageWires.entrySet( ) )
            {
                BundleCapability capability = entry.getKey( );
                Object packageName = capability.getAttributes( ).get( PACKAGE_NAMESPACE );
                Object version = capability.getAttributes( ).get( VERSION_ATTRIBUTE );
                console.print( "        " ).print( packageName ).print( " [" ).print( version ).println( "]" );

                Collection<BundleWire> importers = entry.getValue( );
                if( importers.isEmpty( ) )
                {
                    console.println( "            Not imported by any other bundle." );
                }
                else
                {
                    for( BundleWire wire : importers )
                    {
                        Bundle importer = wire.getRequirerWiring( ).getBundle( );
                        console.print( "            Imported by '" ).print( BundleUtils.toString( importer ) ).println( "'" );
                    }
                }
            }
        }
    }

    private void showImportedPackages( Console console, Bundle bundle ) throws IOException
    {
        BundleRevision revision = bundle.adapt( BundleRevision.class );
        BundleWiring wiring = bundle.adapt( BundleWiring.class );

        Map<BundleRequirement, BundleWire> allPackageWires = new HashMap<>( 30 );

        List<BundleRequirement> packageRequirements = revision.getDeclaredRequirements( PACKAGE_NAMESPACE );
        if( packageRequirements != null )
        {
            for( BundleRequirement requirement : packageRequirements )
            {
                allPackageWires.put( requirement, null );
            }
        }

        List<BundleWire> requiredWires = wiring.getRequiredWires( PACKAGE_NAMESPACE );
        for( BundleWire wire : requiredWires )
        {
            allPackageWires.put( wire.getRequirement( ), wire );
        }

        if( allPackageWires.isEmpty( ) )
        {
            console.println( "NO PACKAGE IMPORTS" );
        }
        else
        {
            console.println( "PACKAGE IMPORTS" );
            for( Map.Entry<BundleRequirement, BundleWire> entry : allPackageWires.entrySet( ) )
            {
                BundleRequirement requirement = entry.getKey( );

                console.print( "        " ).print( requirement.getDirectives( ).get( FILTER_DIRECTIVE ) );

                Object resolution = requirement.getDirectives( ).get( "resolution" );
                if( resolution != null && resolution.toString( ).equalsIgnoreCase( "optional" ) )
                {
                    console.print( " (optional)" );
                }
                console.println( ).print( "            -> " );

                BundleWire wire = entry.getValue( );
                if( wire == null )
                {

                    console.println( "*** UNRESOLVED ***" );

                }
                else
                {
                    BundleWiring providerWiring = wire.getProviderWiring( );
                    String providerSymbolicName = providerWiring.getRevision( ).getSymbolicName( );
                    Version providerVersion = providerWiring.getRevision( ).getVersion( );
                    Object providedPackageName = wire.getCapability( ).getAttributes( ).get( PACKAGE_NAMESPACE );
                    Object providedPackageVersion = wire.getCapability( ).getAttributes( ).get( VERSION_ATTRIBUTE );
                    console.print( providedPackageName ).print( " [" ).print( providedPackageVersion ).print( "] " ).print( " in " ).print( providerSymbolicName ).print( "@" ).print( providerVersion ).println( );
                }
            }
        }
    }

    private void showRequirements( Console console, Bundle bundle ) throws IOException
    {
        BundleRevision revision = bundle.adapt( BundleRevision.class );

        console.println( "DECLARED REQUIREMENTS" );
        for( BundleRequirement requirement : revision.getDeclaredRequirements( null ) )
        {
            console.print( "        " ).println( requirement );
        }
    }

    private void showCapabilities( Console console, Bundle bundle ) throws IOException
    {
        BundleRevision revision = bundle.adapt( BundleRevision.class );
        console.println( "DECLARED CAPABILITIES" );
        for( BundleCapability capability : revision.getDeclaredCapabilities( null ) )
        {
            console.print( "        " ).println( capability );
        }
    }

    private void showMissingRequirements( Console console, BundleStatus status ) throws IOException
    {
        Collection<String> unsatisfiedRequirements = status.getUnsatisfiedRequirements( );
        if( unsatisfiedRequirements != null && !unsatisfiedRequirements.isEmpty( ) )
        {
            console.println( "MISSING REQUIREMENTS" );
            for( String requirement : unsatisfiedRequirements )
            {
                console.print( "        " ).println( requirement );
            }
        }
    }
}
