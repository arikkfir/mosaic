package org.mosaic.server.shell.commands.impl;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import org.mosaic.describe.Description;
import org.mosaic.server.osgi.util.BundleUtils;
import org.mosaic.server.shell.Args;
import org.mosaic.server.shell.Option;
import org.mosaic.server.shell.ShellCommand;
import org.mosaic.server.shell.console.Console;
import org.mosaic.util.logging.Logger;
import org.mosaic.util.logging.LoggerFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.springframework.stereotype.Component;

/**
 * @author arik
 */
@Component
public class InstallBundleCommand extends AbstractCommand
{

    private static final Logger LOG = LoggerFactory.getLogger( InstallBundleCommand.class );

    @Description( "Installs bundle(s) from the given location(s)" )
    @ShellCommand( "install" )
    public void installBundle( Console console,

                               @Option( alias = "s" ) @Description( "show full stack-traces when errors occur" ) boolean stackTraces,

                               @Option( alias = "t" ) @Description( "start the bundles after installation" ) boolean start,

                               @Args String... locations ) throws IOException
    {

        Set<URI> uris = getBundleUris( console, locations );
        if( uris == null )
        {
            return;
        }

        // first install or update all given locations
        Set<Bundle> bundles = new HashSet<>( );
        for( URI uri : uris )
        {
            Bundle bundle = getBundleContext( ).getBundle( uri.toString( ) );
            if( bundle != null )
            {

                // bundle already exists for that location - will attempt to stop and then update it
                // if successful, and the 'start' flag is provided, it will also be started again
                processExistingBundle( console, bundles, uri, bundle );

            }
            else
            {

                // no bundle was installed from that location - install and add to set of bundles to start
                // (if the 'start' flag was provided)
                processNewBundle( console, bundles, uri );

            }
        }

        // only start if all bundles were installed/updated successfully
        if( start )
        {
            if( bundles.size( ) != uris.size( ) )
            {
                console.println( "Not all given locations were processed successfully - will not start any bundles" );
                return;
            }

            // start the bundles
            for( Bundle bundle : bundles )
            {
                try
                {
                    bundle.start( );
                }
                catch( BundleException e )
                {
                    console.print( "Could not start bundle '" ).print( BundleUtils.toString( bundle ) ).println( "'" );
                }
            }
        }
    }

    private void processNewBundle( Console console, Set<Bundle> bundles, URI uri ) throws IOException
    {
        try
        {
            bundles.add( getBundleContext( ).installBundle( uri.toString( ) ) );
        }
        catch( BundleException e )
        {
            LOG.warn( "Could not install bundle from '{}': {}", uri, e.getMessage( ), e );
            console.print( "Could not install bundle from '" ).print( uri ).print( "': " ).println( e.getMessage( ) );
        }
    }

    private Set<URI> getBundleUris( Console console, String[] locations ) throws IOException
    {
        Set<URI> uris = new HashSet<>( );
        for( String location : locations )
        {
            try
            {
                uris.add( new URI( location ) );
            }
            catch( URISyntaxException e )
            {
                String msg = e.getMessage( );
                console.print( "Illegal URI location at: " ).print( location ).print( " (" ).print( msg ).println( ")" );
                return null;
            }
        }
        return uris;
    }

    private void processExistingBundle( Console console, Set<Bundle> bundles, URI uri, Bundle bundle )
    throws IOException
    {
        String bs = BundleUtils.toString( bundle );

        console.print( "Location '" ).print( uri ).println( "' is already installed - will update this bundle." );
        try
        {
            bundle.stop( );

            try
            {
                bundle.update( );
                bundles.add( bundle );
            }
            catch( BundleException e )
            {
                LOG.warn( "Could not update bundle from '{}': {}", uri, e.getMessage( ), e );
                console.print( "Could not update bundle '" ).print( bs ).println( "' - it will not be started" );
            }

        }
        catch( BundleException e )
        {
            LOG.warn( "Could not stop bundle from '{}': {}", uri, e.getMessage( ), e );
            console.print( "Could not stop bundle '" ).print( bs ).println( "' - it will not be updated" );
        }
    }


}
