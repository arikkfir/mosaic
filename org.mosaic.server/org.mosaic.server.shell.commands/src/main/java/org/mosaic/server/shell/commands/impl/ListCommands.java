package org.mosaic.server.shell.commands.impl;

import java.io.IOException;
import java.util.List;
import org.mosaic.describe.Description;
import org.mosaic.lifecycle.BundleState;
import org.mosaic.lifecycle.BundleStatus;
import org.mosaic.server.shell.Args;
import org.mosaic.server.shell.Console;
import org.mosaic.server.shell.Option;
import org.mosaic.server.shell.ShellCommand;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.springframework.stereotype.Component;

/**
 * @author arik
 */
@Component
public class ListCommands extends AbstractCommand {

    @Description( "Lists installed bundles, optionally filtered by name or symbolic name" )
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
            List<String> filters

    ) throws IOException {

        List<Bundle> matches = findMatchingBundles( exact, filters );
        if( matches.isEmpty() ) {
            console.println( "No bundles match requested filters." );
            return;
        }

        Console.TablePrinter table = createTable( console, reqs );
        for( Bundle bundle : matches ) {
            StringBuilder reqsString = new StringBuilder( 100 );

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

            table.print( bundle.getBundleId(),
                         capitalize( state ),
                         bundle.getHeaders().get( Constants.BUNDLE_NAME ),
                         bundle.getSymbolicName(),
                         reqsString );
        }
        table.done();
    }

    private Console.TablePrinter createTable( Console console, boolean reqs ) throws IOException {
        Console.TableHeaders headers =
                console.createTable()
                       .addHeader( "ID", 5 )
                       .addHeader( "State", 10 )
                       .addHeader( "Name", 45 )
                       .addHeader( "Symbolic Name", 50 );
        if( reqs ) {
            headers.addHeader( "Missing requirements", 30 );
        }
        return headers.start();
    }
}
