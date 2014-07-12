package org.mosaic.core.util.version;

import java.util.Objects;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;

/**
 * @author arik
 */
public class VersionRange
{
    @Nonnull
    public static VersionRange valueOf( @Nonnull String range )
    {
        if( range.startsWith( "[" ) && ( range.endsWith( "]" ) || range.endsWith( ")" ) ) )
        {
            boolean upperBoundInclusive = range.endsWith( "]" );
            range = range.substring( 1, range.length() - 1 );

            int delimiterIndex = range.indexOf( ',' );
            if( delimiterIndex != range.lastIndexOf( ',' ) )
            {
                // more than one comma in the range
                throw new IllegalArgumentException( "illegal version range: " + range );
            }
            else if( delimiterIndex < 0 )
            {
                // no comma - a single version range
                if( !upperBoundInclusive )
                {
                    // single-version ranges must end with a "]"
                    throw new IllegalArgumentException( "illegal version range: " + range );
                }
                else
                {
                    return new VersionRange( Version.valueOf( range ), Version.valueOf( range ), true );
                }
            }
            else if( delimiterIndex == range.length() - 1 )
            {
                // ends with a comma - no upper bound
                if( !upperBoundInclusive )
                {
                    // unbounded ranges must end with a "]"
                    throw new IllegalArgumentException( "illegal version range: " + range );
                }
                else
                {
                    String bound = range.substring( 0, delimiterIndex );
                    return new VersionRange( Version.valueOf( bound ), null, true );
                }
            }
            else
            {
                String lowerBound = range.substring( 0, delimiterIndex );
                String upperBound = range.substring( delimiterIndex + 1 );
                return new VersionRange( Version.valueOf( lowerBound ), Version.valueOf( upperBound ), upperBoundInclusive );
            }
        }
        else
        {
            return new VersionRange( Version.valueOf( range ), null, true );
        }
    }

    @Nonnull
    private final Version lowerBound;

    @Nullable
    private final Version upperBound;

    private final boolean upperInclusive;

    private VersionRange( @Nonnull Version lowerBound,
                          @Nullable Version upperBound,
                          boolean upperInclusive )
    {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.upperInclusive = upperInclusive;
    }

    @Nonnull
    public Version getLowerBound()
    {
        return this.lowerBound;
    }

    @Nullable
    public Version getUpperBound()
    {
        return this.upperBound;
    }

    public boolean isUpperInclusive()
    {
        return this.upperInclusive;
    }

    public boolean includes( @Nonnull Version version )
    {
        if( version.compareTo( this.lowerBound ) < 0 )
        {
            return false;
        }
        if( this.upperBound != null )
        {
            if( this.upperInclusive )
            {
                return version.compareTo( this.upperBound ) <= 0;
            }
            else
            {
                return version.compareTo( this.upperBound ) < 0;
            }
        }
        else
        {
            return true;
        }
    }

    @Override
    public String toString()
    {
        return "[" + Objects.toString( this.lowerBound, "" ) + "," + Objects.toString( this.upperBound, "" ) + ( this.upperInclusive ? "]" : ")" );
    }
}
