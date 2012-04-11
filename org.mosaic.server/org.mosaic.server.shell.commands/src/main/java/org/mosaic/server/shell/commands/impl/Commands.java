package org.mosaic.server.shell.commands.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.mosaic.describe.Description;
import org.mosaic.lifecycle.*;
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

    private BundleStatusHelper statusHelper;

    @ServiceRef( required = false )
    public void setStatusHelper( BundleStatusHelper statusHelper ) {
        this.statusHelper = statusHelper;
    }

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

            @Option( alias = "m" )
            @Description( "show missing requirements" )
            boolean reqs,

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
            Console.TableHeaders headers =
                    console.createTable()
                           .addHeader( "ID", 5 )
                           .addHeader( "State", 10 )
                           .addHeader( "Name", 45 )
                           .addHeader( "Symbolic Name", 50 );
            if( reqs ) {
                headers.addHeader( "Missing requirements", 30 );
            }

            Console.TablePrinter table = headers.start();
            StringBuilder reqsString = new StringBuilder( 100 );
            for( Bundle bundle : matches ) {
                reqsString.delete( 0, Integer.MAX_VALUE );

                String bundleName = bundle.getHeaders().get( Constants.BUNDLE_NAME );
                String symbolicName = bundle.getSymbolicName();
                String state;
                if( this.statusHelper != null ) {
                    BundleStatus status = this.statusHelper.getBundleStatus( bundle.getBundleId() );
                    state = status.getState().name();
                    for( String unsatisfied : status.getUnsatisfiedRequirements() ) {
                        if( reqsString.length() > 0 ) {
                            reqsString.append( ',' );
                        }
                        reqsString.append( unsatisfied );
                    }
                } else {
                    state = BundleState.valueOfOsgiState( bundle.getState() ).name();
                }

                state = state.toLowerCase();
                state = Character.toUpperCase( state.charAt( 0 ) ) + state.substring( 1 );

                table.print( bundle.getBundleId(), state, bundleName, symbolicName, reqsString );
            }
            table.done();
        }
    }

}
