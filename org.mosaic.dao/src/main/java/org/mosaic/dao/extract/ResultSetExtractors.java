package org.mosaic.dao.extract;

import com.google.common.reflect.TypeToken;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.*;
import javax.annotation.Nonnull;
import org.joda.time.*;
import org.mosaic.dao.DaoException;

/**
 * @author arik
 */
public class ResultSetExtractors
{
    public static final ResultSetExtractor<BigDecimal> BIGDECIMAL = new ResultSetExtractor<BigDecimal>()
    {
        @Override
        public BigDecimal extract( @Nonnull ResultSet rs, @Nonnull Object... arguments ) throws SQLException
        {
            return rs.getBigDecimal( 1 );
        }
    };

    public static final ResultSetExtractor<BigInteger> BIGINTEGER = new ResultSetExtractor<BigInteger>()
    {
        @Override
        public BigInteger extract( @Nonnull ResultSet rs, @Nonnull Object... arguments ) throws SQLException
        {
            String value = rs.getString( 1 );
            return rs.wasNull() ? null : new BigInteger( value );
        }
    };

    public static final ResultSetExtractor<Boolean> PRIMITIVE_BOOLEAN = new ResultSetExtractor<Boolean>()
    {
        @Override
        public Boolean extract( @Nonnull ResultSet rs, @Nonnull Object... arguments ) throws SQLException
        {
            return rs.getBoolean( 1 );
        }
    };

    public static final ResultSetExtractor<Boolean> BOOLEAN = new ResultSetExtractor<Boolean>()
    {
        @Override
        public Boolean extract( @Nonnull ResultSet rs, @Nonnull Object... arguments ) throws SQLException
        {
            boolean value = rs.getBoolean( 1 );
            return rs.wasNull() ? null : value;
        }
    };

    public static final ResultSetExtractor<Byte> PRIMITIVE_BYTE = new ResultSetExtractor<Byte>()
    {
        @Override
        public Byte extract( @Nonnull ResultSet rs, @Nonnull Object... arguments ) throws SQLException
        {
            return rs.getByte( 1 );
        }
    };

    public static final ResultSetExtractor<Byte> BYTE = new ResultSetExtractor<Byte>()
    {
        @Override
        public Byte extract( @Nonnull ResultSet rs, @Nonnull Object... arguments ) throws SQLException
        {
            byte value = rs.getByte( 1 );
            return rs.wasNull() ? null : value;
        }
    };

    public static final ResultSetExtractor<Character> PRIMITIVE_CHAR = new ResultSetExtractor<Character>()
    {
        @Override
        public Character extract( @Nonnull ResultSet rs, @Nonnull Object... arguments ) throws SQLException
        {
            String value = rs.getString( 1 );
            return value == null ? ( char ) 0 : value.isEmpty() ? ( char ) 0 : value.charAt( 0 );
        }
    };

    public static final ResultSetExtractor<Character> CHAR = new ResultSetExtractor<Character>()
    {
        @Override
        public Character extract( @Nonnull ResultSet rs, @Nonnull Object... arguments ) throws SQLException
        {
            String value = rs.getString( 1 );
            return rs.wasNull() ? null : value.isEmpty() ? null : value.charAt( 0 );
        }
    };

    public static final ResultSetExtractor<Date> DATE = new ResultSetExtractor<Date>()
    {
        @Override
        public Date extract( @Nonnull ResultSet rs, @Nonnull Object... arguments ) throws SQLException
        {
            return rs.getDate( 1 );
        }
    };

    public static final ResultSetExtractor<DateTime> DATETIME = new ResultSetExtractor<DateTime>()
    {
        @Override
        public DateTime extract( @Nonnull ResultSet rs, @Nonnull Object... arguments ) throws SQLException
        {
            Timestamp value = rs.getTimestamp( 1 );
            return rs.wasNull() ? null : new DateTime( value );
        }
    };

    public static final ResultSetExtractor<Double> PRIMITIVE_DOUBLE = new ResultSetExtractor<Double>()
    {
        @Override
        public Double extract( @Nonnull ResultSet rs, @Nonnull Object... arguments ) throws SQLException
        {
            return rs.getDouble( 1 );
        }
    };

    public static final ResultSetExtractor<Double> DOUBLE = new ResultSetExtractor<Double>()
    {
        @Override
        public Double extract( @Nonnull ResultSet rs, @Nonnull Object... arguments ) throws SQLException
        {
            double value = rs.getDouble( 1 );
            return rs.wasNull() ? null : value;
        }
    };

    public static final ResultSetExtractor<Float> PRIMITIVE_FLOAT = new ResultSetExtractor<Float>()
    {
        @Override
        public Float extract( @Nonnull ResultSet rs, @Nonnull Object... arguments ) throws SQLException
        {
            return rs.getFloat( 1 );
        }
    };

    public static final ResultSetExtractor<Float> FLOAT = new ResultSetExtractor<Float>()
    {
        @Override
        public Float extract( @Nonnull ResultSet rs, @Nonnull Object... arguments ) throws SQLException
        {
            float value = rs.getFloat( 1 );
            return rs.wasNull() ? null : value;
        }
    };

    public static final ResultSetExtractor<Instant> INSTANT = new ResultSetExtractor<Instant>()
    {
        @Override
        public Instant extract( @Nonnull ResultSet rs, @Nonnull Object... arguments ) throws SQLException
        {
            Timestamp value = rs.getTimestamp( 1 );
            return rs.wasNull() ? null : new Instant( value );
        }
    };

    public static final ResultSetExtractor<Integer> PRIMITIVE_INT = new ResultSetExtractor<Integer>()
    {
        @Override
        public Integer extract( @Nonnull ResultSet rs, @Nonnull Object... arguments ) throws SQLException
        {
            return rs.getInt( 1 );
        }
    };

    public static final ResultSetExtractor<Integer> INT = new ResultSetExtractor<Integer>()
    {
        @Override
        public Integer extract( @Nonnull ResultSet rs, @Nonnull Object... arguments ) throws SQLException
        {
            int value = rs.getInt( 1 );
            return rs.wasNull() ? null : value;
        }
    };

    public static final ResultSetExtractor<LocalDate> LOCAL_DATE = new ResultSetExtractor<LocalDate>()
    {
        @Override
        public LocalDate extract( @Nonnull ResultSet rs, @Nonnull Object... arguments ) throws SQLException
        {
            Date date = rs.getDate( 1 );
            return date == null ? null : new LocalDate( date );
        }
    };

    public static final ResultSetExtractor<LocalDateTime> LOCAL_DATETIME = new ResultSetExtractor<LocalDateTime>()
    {
        @Override
        public LocalDateTime extract( @Nonnull ResultSet rs, @Nonnull Object... arguments ) throws SQLException
        {
            Timestamp timestamp = rs.getTimestamp( 1 );
            return rs.wasNull() ? null : new LocalDateTime( timestamp );
        }
    };

    public static final ResultSetExtractor<LocalTime> LOCAL_TIME = new ResultSetExtractor<LocalTime>()
    {
        @Override
        public LocalTime extract( @Nonnull ResultSet rs, @Nonnull Object... arguments ) throws SQLException
        {
            Time time = rs.getTime( 1 );
            return rs.wasNull() ? null : new LocalTime( time );
        }
    };

    public static final ResultSetExtractor<Long> PRIMITIVE_LONG = new ResultSetExtractor<Long>()
    {
        @Override
        public Long extract( @Nonnull ResultSet rs, @Nonnull Object... arguments ) throws SQLException
        {
            return rs.getLong( 1 );
        }
    };

    public static final ResultSetExtractor<Long> LONG = new ResultSetExtractor<Long>()
    {
        @Override
        public Long extract( @Nonnull ResultSet rs, @Nonnull Object... arguments ) throws SQLException
        {
            long value = rs.getLong( 1 );
            return rs.wasNull() ? null : value;
        }
    };

    public static final ResultSetExtractor<Short> PRIMITIVE_SHORT = new ResultSetExtractor<Short>()
    {
        @Override
        public Short extract( @Nonnull ResultSet rs, @Nonnull Object... arguments ) throws SQLException
        {
            return rs.getShort( 1 );
        }
    };

    public static final ResultSetExtractor<Short> SHORT = new ResultSetExtractor<Short>()
    {
        @Override
        public Short extract( @Nonnull ResultSet rs, @Nonnull Object... arguments ) throws SQLException
        {
            short value = rs.getShort( 1 );
            return rs.wasNull() ? null : value;
        }
    };

    public static final ResultSetExtractor<String> STRING = new ResultSetExtractor<String>()
    {
        @Override
        public String extract( @Nonnull ResultSet rs, @Nonnull Object... arguments ) throws SQLException
        {
            return rs.getString( 1 );
        }
    };

    public static final ResultSetExtractor<Time> TIME = new ResultSetExtractor<Time>()
    {
        @Override
        public Time extract( @Nonnull ResultSet rs, @Nonnull Object... arguments ) throws SQLException
        {
            return rs.getTime( 1 );
        }
    };

    public static final ResultSetExtractor<Timestamp> TIMESTAMP = new ResultSetExtractor<Timestamp>()
    {
        @Override
        public Timestamp extract( @Nonnull ResultSet rs, @Nonnull Object... arguments ) throws SQLException
        {
            return rs.getTimestamp( 1 );
        }
    };

    @Nonnull
    public static ResultSetExtractor<?> createResultSetExtractorFor( @Nonnull final TypeToken<?> type )
    {
        Class<?> rawType = type.getRawType();
        if( BigDecimal.class.equals( rawType ) )
        {
            return BIGDECIMAL;
        }
        else if( BigInteger.class.equals( rawType ) )
        {
            return BIGINTEGER;
        }
        else if( boolean.class.equals( rawType ) )
        {
            return PRIMITIVE_BOOLEAN;
        }
        else if( Boolean.class.equals( rawType ) )
        {
            return BOOLEAN;
        }
        else if( byte.class.equals( rawType ) )
        {
            return PRIMITIVE_BYTE;
        }
        else if( Byte.class.equals( rawType ) )
        {
            return BYTE;
        }
        else if( char.class.equals( rawType ) )
        {
            return PRIMITIVE_CHAR;
        }
        else if( Character.class.equals( rawType ) )
        {
            return CHAR;
        }
        else if( Date.class.equals( rawType ) )
        {
            return DATE;
        }
        else if( DateTime.class.equals( rawType ) )
        {
            return DATETIME;
        }
        else if( double.class.equals( rawType ) )
        {
            return PRIMITIVE_DOUBLE;
        }
        else if( Double.class.equals( rawType ) )
        {
            return DOUBLE;
        }
        else if( float.class.equals( rawType ) )
        {
            return PRIMITIVE_FLOAT;
        }
        else if( Float.class.equals( rawType ) )
        {
            return FLOAT;
        }
        else if( Instant.class.equals( rawType ) )
        {
            return INSTANT;
        }
        else if( int.class.equals( rawType ) )
        {
            return PRIMITIVE_INT;
        }
        else if( Integer.class.equals( rawType ) )
        {
            return INT;
        }
        else if( LocalDate.class.equals( rawType ) )
        {
            return LOCAL_DATE;
        }
        else if( LocalDateTime.class.equals( rawType ) )
        {
            return LOCAL_DATETIME;
        }
        else if( LocalTime.class.equals( rawType ) )
        {
            return LOCAL_TIME;
        }
        else if( long.class.equals( rawType ) )
        {
            return PRIMITIVE_LONG;
        }
        else if( Long.class.equals( rawType ) )
        {
            return LONG;
        }
        else if( short.class.equals( rawType ) )
        {
            return PRIMITIVE_SHORT;
        }
        else if( Short.class.equals( rawType ) )
        {
            return SHORT;
        }
        else if( String.class.equals( rawType ) )
        {
            return STRING;
        }
        else if( Time.class.equals( rawType ) )
        {
            return TIME;
        }
        else if( Timestamp.class.equals( rawType ) )
        {
            return TIMESTAMP;
        }
        else
        {
            try
            {
                return new BeanResultSetExtractor( type.getRawType().getConstructor( ResultSet.class ) );
            }
            catch( NoSuchMethodException e )
            {
                throw new DaoException( "unable to extract result-set row into '" + type + "'" );
            }
        }
    }

    private static class BeanResultSetExtractor extends ResultSetExtractor<Object>
    {
        @Nonnull
        private final Constructor<?> ctor;

        public BeanResultSetExtractor( @Nonnull Constructor<?> ctor )
        {
            this.ctor = ctor;
            this.ctor.setAccessible( true );
        }

        @Override
        public Object extract( @Nonnull ResultSet rs, @Nonnull Object... arguments ) throws SQLException
        {
            try
            {
                return ctor.newInstance( rs );
            }
            catch( Throwable e )
            {
                throw new SQLException( "could not create '" + this.ctor.getDeclaringClass().getName() + "': " + e.getMessage(), e );
            }
        }
    }
}
