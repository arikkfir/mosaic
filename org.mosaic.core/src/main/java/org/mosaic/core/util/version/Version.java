/*
 * Copyright (c) OSGi Alliance (2004, 2012). All Rights Reserved.
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
package org.mosaic.core.util.version;

import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;

/**
 * Code borrowed thankfully from the OSGi alliance - thank you!
 *
 * @author arik
 */
public final class Version implements Comparable<Version>
{
    @Nonnull
    private static final String SEPARATOR = ".";

    @Nonnull
    private static final Version EMPTY_VERSION = new Version( 0, 0, 0 );

    @Nonnull
    public static Version valueOf( String value )
    {
        if( value == null )
        {
            return EMPTY_VERSION;
        }

        String version = value.trim();
        if( version.length() == 0 )
        {
            return EMPTY_VERSION;
        }
        else if( version.isEmpty() )
        {
            throw new IllegalArgumentException( "invalid version \"" + version + "\": empty string" );
        }

        int dashIndex = version.indexOf( '-' );
        if( dashIndex < 0 )
        {
            String[] tokens = version.split( "\\." );
            switch( tokens.length )
            {
                case 0:
                    throw new IllegalArgumentException( "invalid version \"" + version + "\": no tokens!" );
                case 1:
                    return new Version( parseInt( tokens[ 0 ], version ),
                                        0,
                                        0,
                                        null );
                case 2:
                    return new Version( parseInt( tokens[ 0 ], version ),
                                        parseInt( tokens[ 1 ], version ),
                                        0,
                                        null );
                case 3:
                    return new Version( parseInt( tokens[ 0 ], version ),
                                        parseInt( tokens[ 1 ], version ),
                                        parseInt( tokens[ 2 ], version ),
                                        null );
                default:
                    StringBuilder qualifier = new StringBuilder( 10 );
                    for( int i = 3; i < tokens.length; i++ )
                    {
                        if( qualifier.length() > 0 )
                        {
                            qualifier.append( "." );
                        }
                        qualifier.append( tokens[ i ] );
                    }
                    return new Version( parseInt( tokens[ 0 ], version ),
                                        parseInt( tokens[ 1 ], version ),
                                        parseInt( tokens[ 2 ], version ),
                                        qualifier.toString() );
            }
        }
        else if( version.startsWith( "-" ) || version.endsWith( "-" ) )
        {
            throw new IllegalArgumentException( "invalid version \"" + version + "\": " );
        }
        else
        {
            Version versionWithoutQualifier = valueOf( version.substring( 0, dashIndex ) );
            return new Version( versionWithoutQualifier.getMajor(),
                                versionWithoutQualifier.getMinor(),
                                versionWithoutQualifier.getPatch(),
                                version.substring( dashIndex + 1 ) );
        }
    }

    private static int parseInt( @Nonnull String value, @Nonnull String version )
    {
        try
        {
            return Integer.parseInt( value );
        }
        catch( NumberFormatException e )
        {
            IllegalArgumentException iae = new IllegalArgumentException( "invalid version \"" + version + "\": non-numeric \"" + value + "\"" );
            iae.initCause( e );
            throw iae;
        }
    }

    private final int major;

    private final int minor;

    private final int patch;

    @Nonnull
    private final String qualifier;

    @Nullable
    private String versionString;

    private int hash;

    public Version( int major, int minor, int patch )
    {
        this( major, minor, patch, null );
    }

    public Version( int major, int minor, int patch, @Nullable String qualifier )
    {
        if( qualifier == null )
        {
            qualifier = "";
        }

        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.qualifier = qualifier;
        validate();
    }

    public int getMajor()
    {
        return this.major;
    }

    public int getMinor()
    {
        return this.minor;
    }

    public int getPatch()
    {
        return this.patch;
    }

    @Nonnull
    public String getQualifier()
    {
        return this.qualifier;
    }

    public String toString()
    {
        return toString0();
    }

    public int hashCode()
    {
        if( this.hash != 0 )
        {
            return this.hash;
        }
        int h = 31 * 17;
        h = 31 * h + this.major;
        h = 31 * h + this.minor;
        h = 31 * h + this.patch;
        h = 31 * h + this.qualifier.hashCode();
        return this.hash = h;
    }

    public boolean equals( @Nonnull Object object )
    {
        if( object == this )
        { // quicktest
            return true;
        }

        if( !( object instanceof Version ) )
        {
            return false;
        }

        Version other = ( Version ) object;
        return ( this.major == other.major ) && ( this.minor == other.minor ) && ( this.patch == other.patch ) && this.qualifier.equals( other.qualifier );
    }

    public int compareTo( @Nonnull Version other )
    {
        if( other == this )
        { // quicktest
            return 0;
        }

        int result = this.major - other.major;
        if( result != 0 )
        {
            return result;
        }

        result = this.minor - other.minor;
        if( result != 0 )
        {
            return result;
        }

        result = this.patch - other.patch;
        if( result != 0 )
        {
            return result;
        }

        return this.qualifier.compareTo( other.qualifier );
    }

    String toString0()
    {
        if( this.versionString != null )
        {
            return this.versionString;
        }
        int q = qualifier.length();
        StringBuilder result = new StringBuilder( 20 + q );
        result.append( this.major );
        result.append( SEPARATOR );
        result.append( this.minor );
        result.append( SEPARATOR );
        result.append( this.patch );
        if( q > 0 )
        {
            result.append( SEPARATOR );
            result.append( this.qualifier );
        }
        return this.versionString = result.toString();
    }

    private void validate()
    {
        if( this.major < 0 )
        {
            throw new IllegalArgumentException( "invalid version \"" + toString0() + "\": negative number \"" + this.major + "\"" );
        }
        if( this.minor < 0 )
        {
            throw new IllegalArgumentException( "invalid version \"" + toString0() + "\": negative number \"" + this.minor + "\"" );
        }
        if( this.patch < 0 )
        {
            throw new IllegalArgumentException( "invalid version \"" + toString0() + "\": negative number \"" + this.patch + "\"" );
        }
        for( char ch : this.qualifier.toCharArray() )
        {
            if( ( 'A' <= ch ) && ( ch <= 'Z' ) )
            {
                continue;
            }
            if( ( 'a' <= ch ) && ( ch <= 'z' ) )
            {
                continue;
            }
            if( ( '0' <= ch ) && ( ch <= '9' ) )
            {
                continue;
            }
            if( ( ch == '_' ) || ( ch == '-' ) )
            {
                continue;
            }
            throw new IllegalArgumentException( "invalid version \"" + toString0() + "\": invalid qualifier \"" + this.qualifier + "\"" );
        }
    }
}
