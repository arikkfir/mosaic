/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mosaic.runner.util;

import java.util.HashSet;
import java.util.Set;

/**
 * Copied shamelessly from Spring Framework - copyright is due to authors listed below.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Dave Syer
 */
public abstract class SystemPropertyUtils
{
    private static final PropertyPlaceholderHelper HELPER = new PropertyPlaceholderHelper( );

    public static String resolvePlaceholders( String text )
    {
        PropertyPlaceholderHelper helper = HELPER;
        return helper.replacePlaceholders( text, new SystemPropertyPlaceholderResolver( text ) );
    }

    private static interface PlaceholderResolver
    {
        String resolvePlaceholder( String placeholderName );
    }

    private static class SystemPropertyPlaceholderResolver implements PlaceholderResolver
    {
        private final String text;

        public SystemPropertyPlaceholderResolver( String text )
        {
            this.text = text;
        }

        public String resolvePlaceholder( String placeholderName )
        {
            try
            {
                String propVal = System.getProperty( placeholderName );
                if( propVal == null )
                {
                    propVal = System.getenv( placeholderName );
                }
                return propVal;
            }
            catch( Throwable ex )
            {
                System.err.println( "Could not resolve placeholder '" + placeholderName + "' in [" +
                                    this.text + "] as system property: " + ex );
                return null;
            }
        }
    }

    private static class PropertyPlaceholderHelper
    {
        private final String placeholderPrefix = "${";

        private final String placeholderSuffix = "}";

        private final String simplePrefix = this.placeholderPrefix;

        private final String valueSeparator = ":";

        private final boolean ignoreUnresolvablePlaceholders;

        private PropertyPlaceholderHelper( )
        {
            this.ignoreUnresolvablePlaceholders = false;
        }

        private String replacePlaceholders( String value, PlaceholderResolver placeholderResolver )
        {
            return parseStringValue( value, placeholderResolver, new HashSet<String>( ) );
        }

        private String parseStringValue( String strVal,
                                         PlaceholderResolver placeholderResolver,
                                         Set<String> visitedPlaceholders )
        {

            StringBuilder buf = new StringBuilder( strVal );

            int startIndex = strVal.indexOf( this.placeholderPrefix );
            while( startIndex != -1 )
            {
                int endIndex = findPlaceholderEndIndex( buf, startIndex );
                if( endIndex != -1 )
                {
                    String placeholder = buf.substring( startIndex + this.placeholderPrefix.length( ), endIndex );
                    if( !visitedPlaceholders.add( placeholder ) )
                    {
                        throw new IllegalArgumentException( "Circular placeholder reference '" +
                                                            placeholder +
                                                            "' in property definitions" );
                    }
                    placeholder = parseStringValue( placeholder, placeholderResolver, visitedPlaceholders );
                    String propVal = placeholderResolver.resolvePlaceholder( placeholder );
                    if( propVal == null )
                    {
                        int separatorIndex = placeholder.indexOf( this.valueSeparator );
                        if( separatorIndex != -1 )
                        {
                            String actualPlaceholder = placeholder.substring( 0, separatorIndex );
                            String defaultValue =
                                    placeholder.substring( separatorIndex + this.valueSeparator.length( ) );
                            propVal = placeholderResolver.resolvePlaceholder( actualPlaceholder );
                            if( propVal == null )
                            {
                                propVal = defaultValue;
                            }
                        }
                    }
                    if( propVal != null )
                    {
                        propVal = parseStringValue( propVal, placeholderResolver, visitedPlaceholders );
                        buf.replace( startIndex, endIndex + this.placeholderSuffix.length( ), propVal );
                        startIndex = buf.indexOf( this.placeholderPrefix, startIndex + propVal.length( ) );
                    }
                    else if( this.ignoreUnresolvablePlaceholders )
                    {
                        startIndex = buf.indexOf( this.placeholderPrefix, endIndex + this.placeholderSuffix.length( ) );
                    }
                    else
                    {
                        throw new IllegalArgumentException( "Could not resolve placeholder '" + placeholder + "'" );
                    }

                    visitedPlaceholders.remove( placeholder );
                }
                else
                {
                    startIndex = -1;
                }
            }

            return buf.toString( );
        }

        private int findPlaceholderEndIndex( CharSequence buf, int startIndex )
        {
            int index = startIndex + this.placeholderPrefix.length( );
            int withinNestedPlaceholder = 0;
            while( index < buf.length( ) )
            {
                if( substringMatch( buf, index, this.placeholderSuffix ) )
                {
                    if( withinNestedPlaceholder > 0 )
                    {
                        withinNestedPlaceholder--;
                        index = index + this.placeholderSuffix.length( );
                    }
                    else
                    {
                        return index;
                    }
                }
                else if( substringMatch( buf, index, this.simplePrefix ) )
                {
                    withinNestedPlaceholder++;
                    index = index + this.simplePrefix.length( );
                }
                else
                {
                    index++;
                }
            }
            return -1;
        }

        private static boolean substringMatch( CharSequence str, int index, CharSequence substring )
        {
            for( int j = 0; j < substring.length( ); j++ )
            {
                int i = index + j;
                if( i >= str.length( ) || str.charAt( i ) != substring.charAt( j ) )
                {
                    return false;
                }
            }
            return true;
        }
    }
}
