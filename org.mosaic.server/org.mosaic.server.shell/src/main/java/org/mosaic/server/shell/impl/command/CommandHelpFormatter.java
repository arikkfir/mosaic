package org.mosaic.server.shell.impl.command;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import joptsimple.HelpFormatter;
import joptsimple.OptionDescriptor;
import org.mosaic.server.shell.console.Console;
import org.mosaic.util.logging.Logger;
import org.mosaic.util.logging.LoggerFactory;

/**
 * @author arik
 */
public class CommandHelpFormatter implements HelpFormatter {

    private static final Logger LOG = LoggerFactory.getLogger( CommandHelpFormatter.class );

    private static ThreadLocal<Console> CONSOLE = new ThreadLocal<>();

    private static ThreadLocal<ShellCommand> COMMAND = new ThreadLocal<>();

    public static void reset() {
        CONSOLE.set( null );
        COMMAND.set( null );
    }

    public static void set( ShellCommand command, Console console ) {
        COMMAND.set( command );
        CONSOLE.set( console );
    }

    @Override
    public String format( Map<String, ? extends OptionDescriptor> optionDescriptors ) {
        ShellCommand command = COMMAND.get();
        Console console = CONSOLE.get();
        try {
            console.println()
                   .println( "NAME" )
                   .print( "        " ).print( command.getName() ).print( " - " ).println( command.getDescription() )
                   .println()
                   .println( "ORIGIN" )
                   .print( "        " ).println( command.getOrigin() )
                   .println()
                   .println( "SYNOPSIS" )
                   .print( "        " ).print( command.getName() ).print( " " );

            Map<String, OptionDescriptor> options = new LinkedHashMap<>();
            for( OptionDescriptor descriptor : optionDescriptors.values() ) {
                String shortOption = getShortOption( descriptor );
                if( !options.containsKey( shortOption ) ) {
                    options.put( shortOption, descriptor );
                }
            }
            StringBuilder optionBuf = new StringBuilder( 50 );
            for( OptionDescriptor descriptor : options.values() ) {
                optionBuf.delete( 0, Integer.MAX_VALUE );

                optionBuf.append( '-' ).append( getShortOption( descriptor ) );
                if( descriptor.requiresArgument() ) {
                    optionBuf.append( " <" ).append( descriptor.argumentDescription() ).append( "> " );
                } else if( descriptor.acceptsArguments() ) {
                    optionBuf.append( " [" ).append( descriptor.argumentDescription() ).append( "] " );
                }

                if( descriptor.isRequired() ) {
                    console.print( "< " ).print( optionBuf ).print( " > " );
                } else {
                    console.print( "[ " ).print( optionBuf ).print( " ] " );
                }
            }

            String additionalArgumentsDescription = command.getAdditionalArgumentsDescription();
            if( additionalArgumentsDescription != null ) {
                console.print( "[" ).print( additionalArgumentsDescription ).print( "]" );
            }
            console.println()
                   .println()
                   .println( "OPTIONS" );

            Console.TablePrinter table =
                    console.createTable( 8 )
                           .addHeader( "Options", 20 )
                           .addHeader( "Description", 50 )
                           .start();
            for( Map.Entry<String, OptionDescriptor> entry : options.entrySet() ) {
                OptionDescriptor descriptor = entry.getValue();

                StringBuilder optionNames = new StringBuilder( 100 );
                for( String opt : descriptor.options() ) {
                    if( opt.equals( entry.getKey() ) ) {
                        optionNames.append( "-" ).append( opt );
                    } else {
                        optionNames.append( "--" ).append( opt );
                    }

                    if( descriptor.requiresArgument() ) {
                        optionNames.append( " <" ).append( descriptor.argumentDescription() ).append( "> " );
                    } else if( descriptor.acceptsArguments() ) {
                        optionNames.append( " [" ).append( descriptor.argumentDescription() ).append( "] " );
                    } else {
                        optionNames.append( " " );
                    }
                }

                table.print( optionNames, descriptor.description() );
                table.print( "", "" );
            }
            table.done();

        } catch( IOException e ) {
            try {
                console.printStackTrace( e );
            } catch( IOException e1 ) {
                LOG.error( "Error printing exception: {}", e.getMessage(), e );
            }
        }

        // we're fooling JOpt here - we print to the console ourselves, and return an empty string back so it won't
        // print itself. We're doing this since it's much easier to format the options table using the Console API.
        return "";
    }

    private static String getShortOption( OptionDescriptor desc ) {
        String shortest = null;
        for( String option : desc.options() ) {
            if( shortest == null || option.length() < shortest.length() ) {
                shortest = option;
            }
        }
        return shortest;
    }
}
