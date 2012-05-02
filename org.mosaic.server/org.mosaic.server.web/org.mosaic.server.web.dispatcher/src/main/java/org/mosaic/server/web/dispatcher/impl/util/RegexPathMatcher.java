package org.mosaic.server.web.dispatcher.impl.util;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.util.StringUtils.countOccurrencesOf;
import static org.springframework.util.StringUtils.hasText;

/**
 * @author arik
 */
public class RegexPathMatcher
{
    private static final String DOUBLE_STAR_PATTERN = ".*";

    private static final String SINGLE_STAR_PATTERN = "[^/]+";

    private final String pattern;

    private final Pattern regex;

    private final List<UriToken> pathTokens = new LinkedList<>();

    private final Map<String, Integer> variableIndices = new LinkedHashMap<>();

    private int groupCount = 0;

    public RegexPathMatcher( String path )
    {
        this.pattern = path;

        StringBuilder regex = new StringBuilder( this.pattern.length() * 2 );

        //
        // the "\A" sequence means begining of input
        //
        regex.append( "\\A" );

        //
        // iterate the path, building the regex
        //
        StringBuilder buf = new StringBuilder( 100 );
        int i = 0;
        int pathLength = this.pattern.length();
        boolean collectingPattern = false;
        int patternCurlyNestCount = 0;
        while( i < pathLength )
        {
            char c = this.pattern.charAt( i );
            switch( c )
            {
                case '{':
                    if( collectingPattern )
                    {
                        patternCurlyNestCount++;
                        buf.append( c );
                    }
                    else
                    {
                        regex.append( Pattern.quote( buf.toString() ) );
                        this.pathTokens.add( new UriTokenImpl( buf.toString(), false ) );
                        buf.delete( 0, buf.length() );
                        collectingPattern = true;
                        patternCurlyNestCount = 0;
                    }
                    break;

                case '}':
                    if( collectingPattern )
                    {
                        if( patternCurlyNestCount > 0 )
                        {
                            buf.append( c );
                            patternCurlyNestCount--;
                        }
                        else
                        {
                            regex.append( parseVariablePattern( buf.toString() ) );
                            this.pathTokens.add( new UriTokenImpl( buf.toString(), true ) );
                            buf.delete( 0, buf.length() );
                            collectingPattern = false;
                            patternCurlyNestCount = 0;
                        }
                    }
                    else
                    {
                        buf.append( c );
                    }
                    break;

                case '*':
                    //
                    // if we're not collecting a pattern ("{...}") add a star/double-star pattern.
                    // if WE ARE collecting a pattern, skip this and continue to the 'default' section (NO BREAK!)
                    //
                    if( !collectingPattern )
                    {
                        regex.append( Pattern.quote( buf.toString() ) );
                        this.pathTokens.add( new UriTokenImpl( buf.toString(), false ) );
                        buf.delete( 0, buf.length() );
                        if( i + 1 < pathLength && this.pattern.charAt( i + 1 ) == '*' )
                        {
                            regex.append( DOUBLE_STAR_PATTERN );
                            this.pathTokens.add( new UriTokenImpl( "**", false ) );
                            i++;
                        }
                        else
                        {
                            regex.append( SINGLE_STAR_PATTERN );
                            this.pathTokens.add( new UriTokenImpl( "*", false ) );
                        }
                        break;
                    }

                default:
                    buf.append( c );
            }
            i++;
        }

        //
        // if there's a "{" in the path without a closing "}" - invalid path
        //
        if( collectingPattern )
        {
            throw new IllegalArgumentException( "Illegal path expression: " + this.pattern );
        }
        else if( buf.length() > 0 )
        {
            regex.append( Pattern.quote( buf.toString() ) );
            this.pathTokens.add( new UriTokenImpl( buf.toString(), false ) );
        }

        //
        // the "\Z" means end of input (excluding any line terminators if there are any)
        //
        regex.append( "\\Z" );

        this.regex = Pattern.compile( regex.toString() );
    }

    public List<UriToken> getPathTokens()
    {
        return this.pathTokens;
    }

    public String getPattern()
    {
        return this.pattern;
    }

    public String getRegex()
    {
        return this.regex.pattern();
    }

    public Collection<String> getVariableNames()
    {
        return this.variableIndices.keySet();
    }

    public MatchResult match( String uri )
    {
        return new MatchResult( uri );
    }

    private String parseVariablePattern( String variablePattern )
    {
        String variableName, pattern;

        //
        // name[:pattern]
        //
        int firstColonIndex = variablePattern.indexOf( ':' );
        if( firstColonIndex <= 0 )
        {
            //
            // the variable pattern is "{myVar}" - capture everything for this variable name
            //
            variableName = variablePattern;
            pattern = SINGLE_STAR_PATTERN;
        }
        else
        {
            //
            // the variable pattern is "{myVar:pattern}" - capture using given pattern for this variable name
            //
            variableName = variablePattern.substring( 0, firstColonIndex );
            pattern = variablePattern.substring( firstColonIndex + 1 );
        }

        this.groupCount += 1;
        this.groupCount += countOccurrencesOf( pattern, "(" );
        this.variableIndices.put( variableName, this.groupCount );

        return "(" + pattern + ")";
    }

    public class MatchResult
    {

        private final boolean matching;

        private final Map<String, String> variables;

        private MatchResult( String uri )
        {
            if( !hasText( uri ) )
            {
                this.matching = false;
                this.variables = Collections.emptyMap();
            }
            else
            {
                Matcher matcher = regex.matcher( uri );
                if( matcher.matches() )
                {
                    this.matching = true;
                    this.variables = new HashMap<>();
                    for( Map.Entry<String, Integer> entry : variableIndices.entrySet() )
                    {
                        this.variables.put( entry.getKey(), matcher.group( entry.getValue() ) );
                    }
                }
                else
                {
                    this.matching = false;
                    this.variables = Collections.emptyMap();
                }
            }
        }

        public String getPattern()
        {
            return RegexPathMatcher.this.getPattern();
        }

        public String getRegex()
        {
            return RegexPathMatcher.this.getRegex();
        }

        public boolean isMatching()
        {
            return this.matching;
        }

        public Map<String, String> getVariables()
        {
            return this.variables;
        }
    }

    private static interface UriToken
    {

        String getPath();

        boolean isVariable();

    }

    private static class UriTokenImpl implements UriToken
    {

        private final String path;

        private final boolean variable;

        private UriTokenImpl( String path, boolean variable )
        {
            this.path = path;
            this.variable = variable;
        }

        @Override
        public String getPath()
        {
            return this.path;
        }

        @Override
        public boolean isVariable()
        {
            return this.variable;
        }
    }
}
