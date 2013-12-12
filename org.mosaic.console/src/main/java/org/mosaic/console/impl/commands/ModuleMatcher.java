package org.mosaic.console.impl.commands;

import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Iterables;
import java.util.*;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.modules.Module;
import org.mosaic.modules.ModuleManager;
import org.mosaic.modules.Service;

import static com.google.common.collect.Lists.newArrayList;

/**
 * @author arik
 */
final class ModuleMatcher
{
    @Nonnull
    @Service
    private ModuleManager moduleManager;

    @Nonnull
    Iterable<? extends Module> matchModules( @Nullable final String sortKey, @Nullable String filter )
    {
        List<Module> modulesList;

        if( filter == null )
        {
            modulesList = new ArrayList<>( this.moduleManager.getModules() );
        }
        else
        {
            modulesList = new ArrayList<>( 100 );

            final List<Pattern> patterns = getRegexPatternsFromFilters( filter );
            Iterable<? extends Module> modules = Iterables.filter( this.moduleManager.getModules(), new Predicate<Module>()
            {
                @Override
                public boolean apply( @Nullable Module module )
                {
                    if( module != null )
                    {
                        for( Pattern pattern : patterns )
                        {
                            if( pattern.matcher( module.getName() ).matches() )
                            {
                                return true;
                            }
                        }
                    }
                    return false;
                }
            } );
            for( Module module : modules )
            {
                modulesList.add( module );
            }
        }

        if( sortKey != null )
        {
            Collections.sort( modulesList, new Comparator<Module>()
            {
                @Override
                public int compare( Module o1, Module o2 )
                {
                    switch( sortKey )
                    {
                        case "id":
                            return ComparisonChain.start().compare( o1.getId(), o2.getId() ).result();
                        case "name":
                            return ComparisonChain.start().compare( o1.getName(), o2.getName() ).result();
                        case "version":
                            return ComparisonChain.start().compare( o1.getVersion(), o2.getVersion() ).result();
                        default:
                            return ComparisonChain.start().compare( o1.getId(), o2.getId() ).result();
                    }
                }
            } );
        }
        return modulesList;
    }

    @Nonnull
    private List<Pattern> getRegexPatternsFromFilters( @Nonnull String... filters )
    {
        final List<Pattern> patterns = new LinkedList<>();
        for( String filter : filters )
        {
            if( filter.startsWith( "/" ) )
            {
                int flags = 0;

                List<String> regexTokens = newArrayList( Splitter.on( '/' ).trimResults().split( filter.substring( 1 ) ) );
                switch( regexTokens.size() )
                {
                    case 3:
                        if( !regexTokens.get( 3 ).isEmpty() )
                        {
                            throw new IllegalArgumentException( "Illegal filter: " + filter );
                        }
                        // no break; do the same as if only 2 tokens were given (3 tokens are ok if the 3rd token is empty)

                    case 2:
                        flags = translateRegexCharFlagsToInts( regexTokens.get( 1 ) );
                        // no break; when 2 tokens given, the 2nd is simply the flags, still do the same as if we had only 1 token

                    case 1:
                        //noinspection MagicConstant
                        patterns.add( Pattern.compile( regexTokens.get( 0 ), flags ) );
                }
            }
            else
            {
                // filter did not start with a "/", so just use it as-is
                patterns.add( Pattern.compile( ".*" + Pattern.quote( filter ) + ".*" ) );
            }
        }
        return patterns;
    }

    private int translateRegexCharFlagsToInts( @Nonnull String flagsChars )
    {
        int flags = 0;
        if( flagsChars.contains( "d" ) )
        {
            //noinspection ConstantConditions
            flags = flags | Pattern.UNIX_LINES;
        }
        if( flagsChars.contains( "i" ) )
        {
            flags = flags | Pattern.CASE_INSENSITIVE;
        }
        if( flagsChars.contains( "x" ) )
        {
            flags = flags | Pattern.COMMENTS;
        }
        if( flagsChars.contains( "m" ) )
        {
            flags = flags | Pattern.MULTILINE;
        }
        if( flagsChars.contains( "l" ) )
        {
            flags = flags | Pattern.LITERAL;
        }
        if( flagsChars.contains( "s" ) )
        {
            flags = flags | Pattern.DOTALL;
        }
        if( flagsChars.contains( "u" ) )
        {
            flags = flags | Pattern.UNICODE_CASE;
        }
        if( flagsChars.contains( "c" ) )
        {
            flags = flags | Pattern.CANON_EQ;
        }
        if( flagsChars.contains( "U" ) )
        {
            flags = flags | Pattern.UNICODE_CHARACTER_CLASS;
        }
        return flags;
    }
}
