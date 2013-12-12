package org.mosaic.console.impl;

import com.google.common.collect.Sets;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.pair.Pair;
import org.mosaic.util.text.CharStream;

/**
 * @author arik
 */
final class ParsedCommandLine
{
    @Nonnull
    private final List<String> arguments = new LinkedList<>();

    @Nonnull
    private final List<Pair<String, String>> options = new LinkedList<>();

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
                        this.options.add( Pair.<String, String>of( optionName, null ) );
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
                            this.options.add( Pair.<String, String>of( optionName, stream.readUntil( c ) ) );
                        }
                        else
                        {
                            this.options.add( Pair.<String, String>of( optionName, c + stream.readUntil( ' ' ) ) );
                        }
                    }

                    state = CliParseState.NEUTRAL;
                    break;
                }
            }

        }
        if( state == CliParseState.POST_OPTION_NAME || state == CliParseState.EXPECT_OPTION_VALUE )
        {
            this.options.add( Pair.<String, String>of( optionName, null ) );
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

        Set<String> nameSet = Sets.newHashSet( names );
        for( Pair<String, String> option : this.options )
        {
            if( nameSet.contains( option.getKey() ) )
            {
                if( values == null )
                {
                    values = new LinkedList<>();
                }
                values.add( option.getValue() );
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
}
