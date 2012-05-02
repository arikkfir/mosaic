package org.mosaic.server.web.dispatcher.impl.util;

import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author arik
 */
public class RegexPathMatcherTest extends Assert
{

    @Test
    public void basicTests()
    {
        testPattern( "/arik/kfir", "/arik/kfir", true );
        testPattern( "/arik/kfir", "/arik/notkfir", false );
        testPattern( "/arik/*", "/arik", false );
        testPattern( "/arik/*", "/arik/", false );
        testPattern( "/arik/*", "/arik/kfir", true );
        testPattern( "/arik/*", "/arik/kfir/add", false );
        testPattern( "/arik/*/kfir", "/arik/something/kfir", true );
        testPattern( "/arik/*/kfir", null, false );
        testPattern( "/arik/*/kfir", "", false );
        testPattern( "/arik/**/kfir", "/arik/something/kfir", true );
        testPattern( "/arik/**/kfir", "/arik/some/thing/kfir", true );
        testPattern( "/arik/{var1}/kfir", "/arik/val1/kfir", true, new Variable( "var1", "val1" ) );
        testPattern( "/arik/{var1:\\d+}/kfir", "/arik/val1/kfir", false );
        testPattern( "/arik/{var1:\\d+}/kfir", "/arik/123/kfir", true, new Variable( "var1", "123" ) );
        testPattern( "/arik/{var1:a{4}}/kfir", "/arik/123/kfir", false );
        testPattern( "/arik/{var1:a{4}}/kfir", "/arik/aaaa/kfir", true, new Variable( "var1", "aaaa" ) );
        testPattern( "/{var1:\\d+\\.\\d+}/kfir", "/1.2/kfir", true, new Variable( "var1", "1.2" ) );
        testPattern( "/{major:\\d+}.{minor:\\d+}/kfir", "/1.2/kfir", true, new Variable( "major", "1" ), new Variable( "minor", "2" ) );
        testPattern( "{ver:(/\\d+\\.\\d+)?}/kfir/{var1}", "/1.2/kfir/val1", true, new Variable( "ver", "/1.2" ), new Variable( "var1", "val1" ) );
        testPattern( "{ver:(/\\d+\\.\\d+)?}/kfir", "/kfir", true, new Variable( "ver", null ) );
    }

    private void testPattern( String pattern, String path, boolean shouldMatch, Variable... vars )
    {
        @SuppressWarnings( { "ConstantConditions" } )
        RegexPathMatcher.MatchResult result = new RegexPathMatcher( pattern ).match( path );
        String desc = "Matching of pattern '" +
                      pattern +
                      "' (regex is '" +
                      result.getRegex() +
                      "') with input '" +
                      path +
                      "' ";

        assertEquals( desc +
                      ( shouldMatch ? "should succeed" : "should NOT succeed" ), shouldMatch, result.isMatching() );

        Map<String, String> matchedVars = result.getVariables();

        assertEquals( desc + " variables count", vars.length, matchedVars.size() );
        for( Variable var : vars )
        {
            if( !matchedVars.containsKey( var.name ) )
            {
                fail( desc + " missing variable '" + var.name + "'" );
            }
            else
            {
                assertEquals( desc +
                              " wrong value for var '" +
                              var.name +
                              "'", var.value, matchedVars.get( var.name ) );
            }
        }

    }

    private class Variable
    {

        private final String name;

        private final String value;

        private Variable( String name, String value )
        {
            this.name = name;
            this.value = value;
        }
    }
}
