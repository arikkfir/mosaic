package org.mosaic.util.conversion.impl;

import javax.annotation.Nonnull;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.mosaic.util.conversion.ConversionException;
import org.mosaic.util.conversion.Converter;
import org.mosaic.util.reflection.TypeTokens;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author arik
 */
@SuppressWarnings( "unchecked" )
public class ConvertersGraphTest
{
    @Test
    public void testSingleConverter()
    {
        ConvertersGraph graph = new ConvertersGraph();
        graph.addConverter( new Converter<String, Dog>()
        {
            @Nonnull
            @Override
            public Dog convert( @Nonnull String source )
            {
                switch( source )
                {
                    case "boxer":
                    case "shnautzer":
                        return new Dog( source );
                    default:
                        throw new IllegalArgumentException( "unknown dog breed: " + source );
                }
            }
        } );

        Converter stringToDogConverter = graph.getConverter( TypeTokens.of( String.class ), TypeTokens.of( Dog.class ) );
        assertThat( stringToDogConverter, not( nullValue() ) );
        assertThat( stringToDogConverter.convert( "shnautzer" ), instanceOf( Dog.class ) );
        assertThat( ( Dog ) stringToDogConverter.convert( "boxer" ), equalTo( new Dog( "boxer" ) ) );

        try
        {
            graph.getConverter( TypeTokens.of( CharSequence.class ), TypeTokens.of( Dog.class ) );
            fail( "expected ConversionException" );
        }
        catch( ConversionException e )
        {
            assertThat( e.getSourceType(), CoreMatchers.<Object>equalTo( TypeTokens.of( CharSequence.class ) ) );
            assertThat( e.getTargetType(), CoreMatchers.<Object>equalTo( TypeTokens.of( Dog.class ) ) );
        }

        Converter stringToDog2ndConverter = graph.getConverter( TypeTokens.of( String.class ), TypeTokens.of( Dog.class ) );
        assertThat( stringToDogConverter, is( sameInstance( stringToDog2ndConverter ) ) );

        graph.addConverter( new Converter<Animal, String>()
        {
            @Nonnull
            @Override
            public String convert( @Nonnull Animal source )
            {
                return source.getClass().getSimpleName() + " sounds " + source.getSound();
            }
        } );

        Converter animalToStringConverter = graph.getConverter( TypeTokens.of( Bird.class ), TypeTokens.of( String.class ) );
        assertThat( animalToStringConverter, not( nullValue() ) );
        assertThat( animalToStringConverter.convert( new Bird() ), instanceOf( String.class ) );
        assertThat( ( String ) animalToStringConverter.convert( new Bird() ), equalTo( "Bird sounds shriek" ) );

        Converter animalToCharSeqConverter = graph.getConverter( TypeTokens.of( Bird.class ), TypeTokens.of( CharSequence.class ) );
        assertThat( animalToCharSeqConverter, not( nullValue() ) );
        assertThat( animalToCharSeqConverter.convert( new Bird() ), instanceOf( CharSequence.class ) );
        assertThat( ( CharSequence ) animalToCharSeqConverter.convert( new Bird() ), equalTo( ( CharSequence ) "Bird sounds shriek" ) );
    }
}
