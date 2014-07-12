package org.mosaic.shell.core.impl;

import java.util.*;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;

/**
 * @author arik
 */
final class ParsedCommandLine
{
    @Nonnull
    private final List<String> arguments = new LinkedList<>();

    @Nonnull
    private final List<StringPair> options = new LinkedList<>();

    ParsedCommandLine( @Nonnull String commandLine )
    {
        String optionName = null;
        CliParseState state = CliParseState.NEUTRAL;
        CharStream stream = new CharStream( commandLine );
        while( stream.hasNext() )
        {
            char c = stream.next();
            switch( state )
            {
                case NEUTRAL:
                {
                    if( c != ' ' )
                    {
                        if( c != '-' )
                        {
                            // start reading an argument - read it fully
                            this.arguments.add( c + stream.readUntil( ' ' ) );
                        }
                        else if( !stream.hasNext() )
                        {
                            // looks like start of an option, but since that's the last char, it's treated as an argument
                            // eg. "mycommand arg1 -"
                            this.arguments.add( "-" );
                        }
                        else
                        {
                            state = CliParseState.POST_1ST_HYPHEN;
                        }
                    }
                    break;
                }
                case POST_1ST_HYPHEN:
                {
                    if( c == ' ' )
                    {
                        // a space following a hyphen is no option, treat as a an argument
                        // eg. "mycommand arg1 - arg2"
                        this.arguments.add( "-" );
                        state = CliParseState.NEUTRAL;
                    }
                    else if( c == '-' )
                    {
                        if( stream.hasNext() )
                        {
                            // another hyphen, it's probably a long option
                            state = CliParseState.POST_2ND_HYPHEN;
                        }
                        else
                        {
                            // stream ends here with a double hyphen - just add as an argument
                            // eg. "mycommand arg1 --"
                            this.arguments.add( "--" );
                            state = CliParseState.NEUTRAL;
                        }
                    }
                    else
                    {
                        // a real option?
                        optionName = c + stream.readWhileNotAnyOf( " =:" );
                        state = CliParseState.POST_OPTION_NAME;
                    }
                    break;
                }
                case POST_2ND_HYPHEN:
                {
                    if( c == ' ' )
                    {
                        // a space following double hyphen is no option, treat as an argument
                        this.arguments.add( "--" );
                        state = CliParseState.NEUTRAL;
                    }
                    else if( c == '-' )
                    {
                        // 3rd hyphen! treat as an argument
                        this.arguments.add( "---" + stream.readUntil( ' ' ) );
                        state = CliParseState.NEUTRAL;
                    }
                    else
                    {
                        optionName = c + stream.readWhileNotAnyOf( " =:" );
                        state = CliParseState.POST_OPTION_NAME;
                    }
                    break;
                }
                case POST_OPTION_NAME:
                {
                    if( c == ' ' )
                    {
                        // space after the option name - user just gave us "-p " (mind the space after the "p")
                        // add a null value (signaling no value for this option instance) and return to neutral state
                        this.options.add( new StringPair( optionName, null ) );
                        state = CliParseState.NEUTRAL;
                    }
                    else
                    {
                        // we know it's either "=" or ":" -> see call to "readWhileNotAnyOf in previous state
                        state = CliParseState.EXPECT_OPTION_VALUE;
                    }
                    break;
                }
                case EXPECT_OPTION_VALUE:
                {
                    if( c != ' ' )
                    {
                        if( c == '"' || c == '\'' )
                        {
                            this.options.add( new StringPair( optionName, stream.readUntil( c ) ) );
                        }
                        else
                        {
                            this.options.add( new StringPair( optionName, c + stream.readUntil( ' ' ) ) );
                        }
                    }

                    state = CliParseState.NEUTRAL;
                    break;
                }
            }

        }
        if( state == CliParseState.POST_OPTION_NAME || state == CliParseState.EXPECT_OPTION_VALUE )
        {
            this.options.add( new StringPair( optionName, null ) );
        }
    }

    @Nullable
    String getArgumentValue( int index )
    {
        if( index >= this.arguments.size() )
        {
            return null;
        }
        else
        {
            return this.arguments.get( index );
        }
    }

    @Nullable
    List<String> getOptionValues( @Nonnull String... names )
    {
        List<String> values = null;

        Set<String> nameSet = new HashSet<String>( Arrays.<String>asList( names ) );
        for( StringPair option : this.options )
        {
            if( nameSet.contains( option.left ) )
            {
                if( values == null )
                {
                    values = new LinkedList<>();
                }
                values.add( option.right );
            }
        }
        return values;
    }

    private static enum CliParseState
    {
        NEUTRAL,
        POST_1ST_HYPHEN,
        POST_2ND_HYPHEN,
        POST_OPTION_NAME,
        EXPECT_OPTION_VALUE
    }

    private static class StringPair
    {
        @Nullable
        private final String left;

        @Nullable
        private final String right;

        private StringPair( @Nullable String left, @Nullable String right )
        {
            this.left = left;
            this.right = right;
        }
    }
}
