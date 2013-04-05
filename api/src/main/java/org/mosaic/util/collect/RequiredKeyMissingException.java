package org.mosaic.util.collect;

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

    public RequiredKeyMissingException( String message, Throwable cause, @Nonnull Object key )
    {
        super( message, cause );
        this.key = key;
    }

    public RequiredKeyMissingException( Throwable cause, @Nonnull Object key )
    {
        super( cause );
        this.key = key;
    }

    @Nonnull
    public Object getKey()
    {
        return key;
    }
}
