package org.mosaic.util.conversion.impl;

import javax.annotation.Nonnull;
import org.mosaic.util.conversion.Converter;

/**
 * @author arik
 */
public class Converters
{
    public static class StringToBooleanConverter implements Converter<String, Boolean>
    {
        @Nonnull
        @Override
        public Boolean convert( @Nonnull String s )
        {
            return Boolean.valueOf( s );
        }
    }

    public static class StringToInteger implements Converter<String, Integer>
    {
        @Nonnull
        @Override
        public Integer convert( @Nonnull String s )
        {
            return Integer.valueOf( s );
        }
    }
}
