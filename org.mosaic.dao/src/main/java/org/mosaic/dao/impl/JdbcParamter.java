package org.mosaic.dao.impl;

import com.google.common.base.Optional;
import com.google.common.primitives.Primitives;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.ReadableInstant;
import org.mosaic.dao.DaoException;
import org.mosaic.dao.JdbcType;
import org.mosaic.util.method.MethodParameter;

/**
 * @author arik
 */
final class JdbcParamter
{
    private final int jdbcParameterIndex;

    @Nonnull
    private final Integer jdbcType;

    JdbcParamter( @Nonnull MethodParameter methodParameter, int index )
    {
        this.jdbcParameterIndex = index;

        Optional<JdbcType> jdbcTypeHolder = methodParameter.getAnnotation( JdbcType.class );
        if( jdbcTypeHolder.isPresent() )
        {
            this.jdbcType = jdbcTypeHolder.get().value();
        }
        else
        {
            Class<?> type = Primitives.wrap( methodParameter.getType().getRawType() );
            if( BigDecimal.class.equals( type ) )
            {
                this.jdbcType = Types.NUMERIC;
            }
            else if( BigInteger.class.equals( type ) || Long.class.equals( type ) )
            {
                this.jdbcType = Types.BIGINT;
            }
            else if( Boolean.class.equals( type ) )
            {
                this.jdbcType = Types.BOOLEAN;
            }
            else if( Byte.class.equals( type ) )
            {
                this.jdbcType = Types.TINYINT;
            }
            else if( Character.class.equals( type ) )
            {
                this.jdbcType = Types.CHAR;
            }
            else if( Double.class.equals( type ) )
            {
                this.jdbcType = Types.DOUBLE;
            }
            else if( Float.class.equals( type ) )
            {
                this.jdbcType = Types.FLOAT;
            }
            else if( Integer.class.equals( type ) )
            {
                this.jdbcType = Types.INTEGER;
            }
            else if( Short.class.equals( type ) )
            {
                this.jdbcType = Types.SMALLINT;
            }
            else if( String.class.equals( type ) || char[].class.equals( type ) )
            {
                this.jdbcType = Types.VARCHAR;
            }
            else if( Blob.class.equals( type ) || byte[].class.equals( type ) )
            {
                this.jdbcType = Types.BLOB;
            }
            else if( Clob.class.equals( type ) )
            {
                this.jdbcType = Types.CLOB;
            }
            else if( Date.class.equals( type ) || LocalDate.class.isAssignableFrom( type ) )
            {
                this.jdbcType = Types.DATE;
            }
            else if( Time.class.equals( type ) || LocalTime.class.isAssignableFrom( type ) )
            {
                this.jdbcType = Types.TIME;
            }
            else if( Timestamp.class.equals( type ) || ReadableInstant.class.isAssignableFrom( type ) || LocalDateTime.class.isAssignableFrom( type ) )
            {
                this.jdbcType = Types.TIMESTAMP;
            }
            else
            {
                throw new DaoException( "parameter '" + methodParameter.getName() + "' of method '" + methodParameter.getMethod().getName() + "' in @Dao '" + methodParameter.getMethod().getDeclaringClass().getSimpleName() + "' has an unknown type mapping" );
            }
        }
    }

    void apply( @Nonnull PreparedStatement stmt, @Nullable Object value ) throws SQLException
    {
        if( value == null )
        {
            stmt.setNull( this.jdbcParameterIndex, this.jdbcType );
        }
        else
        {
            stmt.setObject( this.jdbcParameterIndex, value, this.jdbcType );
        }
    }
}
