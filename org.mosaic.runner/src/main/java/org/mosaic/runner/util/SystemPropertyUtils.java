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

/**
 * Copied shamelessly from Spring Framework - copyright is due to authors listed below.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Dave Syer
 */
public abstract class SystemPropertyUtils {

    public static final String PLACEHOLDER_PREFIX = "${";

    public static final String PLACEHOLDER_SUFFIX = "}";

    public static final String VALUE_SEPARATOR = ":";

    private static final PropertyPlaceholderHelper strictHelper =
            new PropertyPlaceholderHelper( PLACEHOLDER_PREFIX, PLACEHOLDER_SUFFIX, VALUE_SEPARATOR, false );

    private static final PropertyPlaceholderHelper nonStrictHelper =
            new PropertyPlaceholderHelper( PLACEHOLDER_PREFIX, PLACEHOLDER_SUFFIX, VALUE_SEPARATOR, true );

    public static String resolvePlaceholders( String text, boolean ignoreUnresolvablePlaceholders ) {
        PropertyPlaceholderHelper helper = ( ignoreUnresolvablePlaceholders
                                                     ? nonStrictHelper
                                                     : strictHelper );
        return helper.replacePlaceholders( text, new SystemPropertyPlaceholderResolver( text ) );
    }

    private static class SystemPropertyPlaceholderResolver implements PropertyPlaceholderHelper.PlaceholderResolver {

        private final String text;

        public SystemPropertyPlaceholderResolver( String text ) {
            this.text = text;
        }

        public String resolvePlaceholder( String placeholderName ) {
            try {
                String propVal = System.getProperty( placeholderName );
                if( propVal == null ) {
                    // Fall back to searching the system environment.
                    propVal = System.getenv( placeholderName );
                }
                return propVal;
            } catch( Throwable ex ) {
                System.err.println( "Could not resolve placeholder '" + placeholderName + "' in [" +
                                    this.text + "] as system property: " + ex );
                return null;
            }
        }
    }
}
