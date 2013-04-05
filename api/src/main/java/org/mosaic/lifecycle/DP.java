package org.mosaic.lifecycle;

/**
 * @author arik
 */
public class DP
{
    public static DP dp( String key, Object value )
    {
        return new DP( key, value );
    }

    private final String key;

    private final Object value;

    public DP( String key, Object value )
    {
        this.key = key;
        this.value = value;
    }

    public String getKey()
    {
        return key;
    }

    public Object getValue()
    {
        return value;
    }
}
