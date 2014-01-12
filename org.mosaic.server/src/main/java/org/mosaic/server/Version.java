package org.mosaic.server;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public final class Version implements Comparable<Version>
{
    private static final Pattern MAVEN_VERSION_PATTERN = Pattern.compile( "\\d+(?:\\.\\d+)*(?:\\-\\p{ASCII}+)?" );

    @Nonnull
    private static org.osgi.framework.Version createOsgiVersion( @Nonnull String version )
    {
        Matcher matcher = MAVEN_VERSION_PATTERN.matcher( version );
        if( matcher.matches() )
        {
            int dashIndex = version.indexOf( '-' );
            if( dashIndex >= 0 )
            {
                version = version.replaceFirst( "\\-", "." );
            }
        }
        return new org.osgi.framework.Version( version );
    }

    @Nonnull
    private final org.osgi.framework.Version version;

    public Version( @Nonnull String version )
    {
        this( createOsgiVersion( version ) );
    }

    public Version( int major, int minor, int micro )
    {
        this( new org.osgi.framework.Version( major, minor, micro ) );
    }

    public Version( int major, int minor, int micro, @Nullable String qualifier )
    {
        this( new org.osgi.framework.Version( major, minor, micro, qualifier ) );
    }

    private Version( @Nonnull org.osgi.framework.Version osgiVersion )
    {
        this.version = osgiVersion;
    }

    @Override
    public int hashCode()
    {
        return this.version.hashCode();
    }

    @Override
    public boolean equals( Object o )
    {
        if( this == o )
        {
            return true;
        }
        if( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        Version that = ( Version ) o;
        return this.version.equals( that.version );
    }

    @Override
    public String toString()
    {
        return this.version.toString();
    }

    public final int getMajor()
    {
        return this.version.getMajor();
    }

    public final int getMinor()
    {
        return this.version.getMinor();
    }

    public final int getMicro()
    {
        return this.version.getMicro();
    }

    @Nonnull
    public final String getQualifier()
    {
        return this.version.getQualifier();
    }

    @Override
    public int compareTo( @Nonnull Version that )
    {
        return this.version.compareTo( that.version );
    }
}
