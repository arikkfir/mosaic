package org.mosaic.server.shell.commands.impl;

import java.io.IOException;
import java.util.List;
import org.mosaic.describe.Description;
import org.mosaic.server.shell.Args;
import org.mosaic.server.shell.Option;
import org.mosaic.server.shell.ShellCommand;
import org.mosaic.server.shell.console.Console;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.springframework.stereotype.Component;

import static org.mosaic.osgi.util.BundleUtils.filterBundlesByState;
import static org.mosaic.osgi.util.BundleUtils.findMatchingBundles;
import static org.osgi.framework.Bundle.*;

/**
 * @author arik
 */
@Component
public class UninstallBundleCommand extends AbstractCommand {

    @Description( "Uninstalls the given bundle(s)" )
    @ShellCommand( "uninstall" )
    public void uninstallBundles( Console console,

                                  @Option( alias = "e" )
                                  @Description( "exact matching (filter arguments will not be treated as wildcards)" )
                                  boolean exact,

                                  @Option( alias = "s" )
                                  @Description( "show full stack-traces when errors occur" )
                                  boolean stackTraces,

                                  @Args
                                  String... filters ) throws IOException {

        List<Bundle> matchingBundles = findMatchingBundles( getBundleContext(), exact, filters );
        if( matchingBundles.isEmpty() ) {
            console.println( "No bundles match requested filters." );
            return;
        }

        List<Bundle> matches = filterBundlesByState( matchingBundles, INSTALLED, RESOLVED, ACTIVE );
        if( matches.isEmpty() ) {
            console.println( "None of the matching bundles is in a start-able state (installed/resolved/active)" );
            return;
        }

        Console.TablePrinter table = createBundlesTable( console );
        for( Bundle bundle : matches ) {
            table.print( bundle.getBundleId(),
                         capitalize( getBundleStatus( bundle ).getState().name() ),
                         bundle.getHeaders().get( Constants.BUNDLE_NAME ),
                         bundle.getSymbolicName() );
        }
        table.done();

        if( console.ask( "Uninstall these bundles? [Y/n]", 'y', 'n' ) == 'y' ) {
            for( Bundle bundle : matches ) {
                try {
                    bundle.uninstall();
                } catch( BundleException e ) {
                    if( stackTraces ) {
                        console.printStackTrace( e );
                    } else {
                        console.println( e.getMessage() );
                    }
                }
            }
        }
    }

    private Console.TablePrinter createBundlesTable( Console console ) throws IOException {
        return console.createTable()
                      .addHeader( "ID", 5 )
                      .addHeader( "State", 10 )
                      .addHeader( "Name", 45 )
                      .addHeader( "Symbolic Name", 50 )
                      .start();
    }
}
