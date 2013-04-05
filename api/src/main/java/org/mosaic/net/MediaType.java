package org.mosaic.net;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public class MediaType implements Cloneable, Serializable
{
    private final com.google.common.net.MediaType mediaType;

    public MediaType( @Nonnull String mediaType )
    {
        this.mediaType = com.google.common.net.MediaType.parse( mediaType );
    }

    public MediaType( @Nonnull String type, @Nonnull String subtype )
    {
        this.mediaType = com.google.common.net.MediaType.create( type, subtype );
    }

    private MediaType( @Nonnull com.google.common.net.MediaType mediaType )
    {
        this.mediaType = mediaType;
    }

    @Nonnull
    public String getType()
    {
        return this.mediaType.type();
    }

    @Nonnull
    public String getSubType()
    {
        return this.mediaType.subtype();
    }

    @Nonnull
    public Map<String, Collection<String>> getParameters()
    {
        return this.mediaType.parameters().asMap();
    }

    @Nullable
    public Charset getCharset()
    {
        return this.mediaType.charset().orNull();
    }

    @Nonnull
    public MediaType withoutParameters()
    {
        return new MediaType( this.mediaType.withoutParameters() );
    }

    @Nonnull
    public MediaType withParameter( @Nonnull String key, @Nonnull String value )
    {
        return new MediaType( this.mediaType.withParameter( key, value ) );
    }

    @Nonnull
    public MediaType withCharset( @Nonnull Charset charset )
    {
        return new MediaType( this.mediaType.withCharset( charset ) );
    }

    public boolean hasWildcard()
    {
        return this.mediaType.hasWildcard();
    }

    public boolean is( @Nonnull MediaType mediaTypeRange )
    {
        return this.mediaType.is( mediaTypeRange.mediaType );
    }

    @Override
    public int hashCode()
    {
        return this.mediaType.hashCode();
    }

    @Override
    public boolean equals( Object obj )
    {
        if( obj instanceof MediaType )
        {
            MediaType other = ( MediaType ) obj;
            return this.mediaType.equals( other.mediaType );
        }
        return false;
    }

    @Override
    public Object clone() throws CloneNotSupportedException
    {
        return new MediaType( com.google.common.net.MediaType.parse( toString() ) );
    }

    @Override
    public String toString()
    {
        return this.mediaType.toString();
    }
}
