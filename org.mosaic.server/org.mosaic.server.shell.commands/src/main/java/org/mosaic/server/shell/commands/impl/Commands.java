package org.mosaic.server.shell.commands.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.mosaic.describe.Description;
import org.mosaic.lifecycle.BundleContextAware;
import org.mosaic.server.shell.Args;
import org.mosaic.server.shell.Console;
import org.mosaic.server.shell.Option;
import org.mosaic.server.shell.ShellCommand;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.springframework.stereotype.Component;

/**
 * @author arik
 */
@Component
public class Commands implements BundleContextAware {

    private BundleContext bundleContext;

    @Override
    public void setBundleContext( BundleContext bundleContext ) {
        this.bundleContext = bundleContext;
    }

    @ShellCommand( "list" )
    public void listBundles(

            Console console,

            @Option( alias = "e" )
            @Description( "exact matching (filter arguments will not be treated as wildcards)" )
            boolean exact,

            @Args
            List<String> args

    ) throws IOException {

        Bundle[] bundles = this.bundleContext.getBundles();
        if( bundles == null ) {
            console.println( "No bundles are installed (huh!!?)." );
            return;
        }

        List<Bundle> matches = new ArrayList<>( bundles.length );
        for( Bundle bundle : bundles ) {
            String bundleName = bundle.getHeaders().get( Constants.BUNDLE_NAME );
            String symbolicName = bundle.getSymbolicName();

            boolean match = true;
            if( args != null && !args.isEmpty() ) {
                match = false;
                for( String arg : args ) {
                    if( exact && ( bundleName.equalsIgnoreCase( arg ) || symbolicName.equalsIgnoreCase( arg ) ) ) {
                        match = true;
                        break;
                    } else if( !exact && ( bundleName.contains( arg ) || symbolicName.contains( arg ) ) ) {
                        match = true;
                        break;
                    }
                }
            }

            if( match ) {
                matches.add( bundle );
            }
        }

        if( matches.isEmpty() ) {
            console.println( "No bundles match requested filters." );
        } else {
            Console.TablePrinter table =
                    console.createTable()
                           .addHeader( "ID", 5 )
                           .addHeader( "Name", 45 )
                           .addHeader( "Symbolic Name", 50 )
                           .start();
            for( Bundle bundle : matches ) {
                String bundleName = bundle.getHeaders().get( Constants.BUNDLE_NAME );
                String symbolicName = bundle.getSymbolicName();
                table.print( bundle.getBundleId(), bundleName, symbolicName );
            }
            table.done();
        }
    }

}
