package org.mosaic.server.shell.commands.impl;

import java.io.IOException;
import java.util.List;
import org.mosaic.server.osgi.BundleStatus;
import org.mosaic.server.shell.Args;
import org.mosaic.server.shell.Description;
import org.mosaic.server.shell.Option;
import org.mosaic.server.shell.ShellCommand;
import org.mosaic.server.shell.console.Console;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.springframework.stereotype.Component;

import static org.mosaic.server.osgi.util.BundleUtils.findMatchingBundles;

/**
 * @author arik
 */
@Component
public class ListCommands extends AbstractCommand
{

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
            String... filters )

    throws IOException
    {

        List<Bundle> matches = findMatchingBundles( getBundleContext(), exact, filters );
        if( matches.isEmpty() )
        {
            console.println( "No bundles match requested filters." );
            return;
        }

        Console.TableHeaders headers =
                console.createTable().addHeader( "ID", 5 ).addHeader( "State", 10 ).addHeader( "Name", 45 ).addHeader( "Symbolic Name", 50 );
        if( reqs )
        {
            headers.addHeader( "Missing requirements", 30 );
        }
        Console.TablePrinter table = headers.start();

        for( Bundle bundle : matches )
        {
            BundleStatus status = getBundleStatus( bundle );

            StringBuilder reqsString = new StringBuilder( 100 );
            for( String unsatisfied : status.getUnsatisfiedRequirements() )
            {
                if( reqsString.length() > 0 )
                {
                    reqsString.append( ',' );
                }
                reqsString.append( unsatisfied );
            }

            table.print( bundle.getBundleId(), capitalize( status.getState().name() ), bundle.getHeaders().get( Constants.BUNDLE_NAME ), bundle.getSymbolicName(), reqsString );
        }
        table.done();
    }
}
