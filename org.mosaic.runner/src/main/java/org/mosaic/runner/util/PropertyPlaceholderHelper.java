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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Copied shamelessly from Spring Framework - copyright is due to authors listed below.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 */
public class PropertyPlaceholderHelper {

    private static final Map<String, String> wellKnownSimplePrefixes = new HashMap<>( 4 );

    static {
        wellKnownSimplePrefixes.put( "}", "{" );
        wellKnownSimplePrefixes.put( "]", "[" );
        wellKnownSimplePrefixes.put( ")", "(" );
    }


    private final String placeholderPrefix;

    private final String placeholderSuffix;

    private final String simplePrefix;

    private final String valueSeparator;

    private final boolean ignoreUnresolvablePlaceholders;

    public PropertyPlaceholderHelper( String placeholderPrefix, String placeholderSuffix,
                                      String valueSeparator, boolean ignoreUnresolvablePlaceholders ) {
        this.placeholderPrefix = placeholderPrefix;
        this.placeholderSuffix = placeholderSuffix;
        String simplePrefixForSuffix = wellKnownSimplePrefixes.get( this.placeholderSuffix );
        if( simplePrefixForSuffix != null && this.placeholderPrefix.endsWith( simplePrefixForSuffix ) ) {
            this.simplePrefix = simplePrefixForSuffix;
        } else {
            this.simplePrefix = this.placeholderPrefix;
        }
        this.valueSeparator = valueSeparator;
        this.ignoreUnresolvablePlaceholders = ignoreUnresolvablePlaceholders;
    }

    public String replacePlaceholders( String value, PlaceholderResolver placeholderResolver ) {
        return parseStringValue( value, placeholderResolver, new HashSet<String>() );
    }

    protected String parseStringValue(
            String strVal, PlaceholderResolver placeholderResolver, Set<String> visitedPlaceholders ) {

        StringBuilder buf = new StringBuilder( strVal );

        int startIndex = strVal.indexOf( this.placeholderPrefix );
        while( startIndex != -1 ) {
            int endIndex = findPlaceholderEndIndex( buf, startIndex );
            if( endIndex != -1 ) {
                String placeholder = buf.substring( startIndex + this.placeholderPrefix.length(), endIndex );
                if( !visitedPlaceholders.add( placeholder ) ) {
                    throw new IllegalArgumentException(
                            "Circular placeholder reference '" + placeholder + "' in property definitions" );
                }
                // Recursive invocation, parsing placeholders contained in the placeholder key.
                placeholder = parseStringValue( placeholder, placeholderResolver, visitedPlaceholders );

                // Now obtain the value for the fully resolved key...
                String propVal = placeholderResolver.resolvePlaceholder( placeholder );
                if( propVal == null && this.valueSeparator != null ) {
                    int separatorIndex = placeholder.indexOf( this.valueSeparator );
                    if( separatorIndex != -1 ) {
                        String actualPlaceholder = placeholder.substring( 0, separatorIndex );
                        String defaultValue = placeholder.substring( separatorIndex + this.valueSeparator.length() );
                        propVal = placeholderResolver.resolvePlaceholder( actualPlaceholder );
                        if( propVal == null ) {
                            propVal = defaultValue;
                        }
                    }
                }
                if( propVal != null ) {
                    // Recursive invocation, parsing placeholders contained in the
                    // previously resolved placeholder value.
                    propVal = parseStringValue( propVal, placeholderResolver, visitedPlaceholders );
                    buf.replace( startIndex, endIndex + this.placeholderSuffix.length(), propVal );
                    startIndex = buf.indexOf( this.placeholderPrefix, startIndex + propVal.length() );
                } else if( this.ignoreUnresolvablePlaceholders ) {
                    // Proceed with unprocessed value.
                    startIndex = buf.indexOf( this.placeholderPrefix, endIndex + this.placeholderSuffix.length() );
                } else {
                    throw new IllegalArgumentException( "Could not resolve placeholder '" + placeholder + "'" );
                }

                visitedPlaceholders.remove( placeholder );
            } else {
                startIndex = -1;
            }
        }

        return buf.toString();
    }

    private int findPlaceholderEndIndex( CharSequence buf, int startIndex ) {
        int index = startIndex + this.placeholderPrefix.length();
        int withinNestedPlaceholder = 0;
        while( index < buf.length() ) {
            if( substringMatch( buf, index, this.placeholderSuffix ) ) {
                if( withinNestedPlaceholder > 0 ) {
                    withinNestedPlaceholder--;
                    index = index + this.placeholderSuffix.length();
                } else {
                    return index;
                }
            } else if( substringMatch( buf, index, this.simplePrefix ) ) {
                withinNestedPlaceholder++;
                index = index + this.simplePrefix.length();
            } else {
                index++;
            }
        }
        return -1;
    }


    /**
     * Strategy interface used to resolve replacement values for placeholders contained in Strings.
     *
     * @see PropertyPlaceholderHelper
     */
    public static interface PlaceholderResolver {

        /**
         * Resolves the supplied placeholder name into the replacement value.
         *
         * @param placeholderName the name of the placeholder to resolve.
         * @return the replacement value or <code>null</code> if no replacement is to be made.
         */
        String resolvePlaceholder( String placeholderName );
    }

    public static boolean substringMatch( CharSequence str, int index, CharSequence substring ) {
        for( int j = 0; j < substring.length(); j++ ) {
            int i = index + j;
            if( i >= str.length() || str.charAt( i ) != substring.charAt( j ) ) {
                return false;
            }
        }
        return true;
    }
}
