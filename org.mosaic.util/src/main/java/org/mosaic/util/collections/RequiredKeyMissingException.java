package org.mosaic.util.collections;

import javax.annotation.Nonnull;

/**
 * @author arik
 */
public class RequiredKeyMissingException extends IllegalStateException
{
    @Nonnull
    private final Object key;

    public RequiredKeyMissingException( @Nonnull Object key )
    {
        this.key = key;
    }

    public RequiredKeyMissingException( String s, @Nonnull Object key )
    {
        super( s );
        this.key = key;
    }

    @Nonnull
    public Object getKey()
    {
        return key;
    }
}
