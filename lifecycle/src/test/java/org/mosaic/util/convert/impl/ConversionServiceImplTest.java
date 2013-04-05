package org.mosaic.util.convert.impl;

import com.google.common.reflect.TypeToken;
import java.util.Date;
import javax.annotation.Nonnull;
import org.junit.Assert;
import org.junit.Test;
import org.mosaic.util.convert.Converter;
import org.osgi.framework.InvalidSyntaxException;

/**
 * @author arik
 */
public class ConversionServiceImplTest
{
    @Test
    public void testConversionWithRedundantConverters() throws InvalidSyntaxException
    {
        ConversionServiceImpl conversionService = new ConversionServiceImpl();
        conversionService.registerConverter( new Converter<String, Integer>()
        {
            @Nonnull
            @Override
            public Integer convert( @Nonnull String source )
            {
                return Integer.parseInt( source );
            }
        } );
        conversionService.registerConverter( new Converter<Integer, Long>()
        {
            @Nonnull
            @Override
            public Long convert( @Nonnull Integer source )
            {
                return source.longValue();
            }
        } );
        conversionService.registerConverter( new Converter<Long, Date>()
        {
            @Nonnull
            @Override
            public Date convert( @Nonnull Long source )
            {
                return new Date( source );
            }
        } );

        long nowMillis = System.currentTimeMillis();
        Date nowDate = new Date( nowMillis );

        Date date = conversionService.convert( nowMillis, TypeToken.of( Date.class ) );
        Assert.assertEquals( nowDate, date );
    }

    @Test
    public void testConversionFromSupertype() throws InvalidSyntaxException
    {
        ConversionServiceImpl conversionService = new ConversionServiceImpl();
        conversionService.registerConverter( new Converter<Object, String>()
        {
            @Nonnull
            @Override
            public String convert( @Nonnull Object source )
            {
                return source.toString();
            }
        } );

        Object source = new Object()
        {
            @Override
            public String toString()
            {
                return "MyObject";
            }
        };

        Assert.assertEquals( "MyObject", conversionService.convert( source, TypeToken.of( String.class ) ) );
        Assert.assertEquals( "MyObject", conversionService.convert( source, TypeToken.of( String.class ) ) );
    }
}
