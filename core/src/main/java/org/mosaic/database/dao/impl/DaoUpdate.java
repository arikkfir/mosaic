package org.mosaic.database.dao.impl;

import java.sql.SQLException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.sql.DataSource;
import org.mosaic.database.dao.UpdateResult;
import org.mosaic.database.dao.annotation.Update;
import org.mosaic.database.tx.TransactionManager;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.util.reflection.MethodHandle;

/**
 * @author arik
 */
public class DaoUpdate extends BaseDaoAction
{
    @Nonnull
    private final JdbcTemplate jdbcTemplate;

    @Nullable
    private final Integer minRequiredAffectedRows;

    @Nullable
    private final Integer maxRequiredAffectedRows;

    public DaoUpdate( @Nonnull TransactionManager transactionManager,
                      @Nonnull ConversionService conversionService,
                      @Nonnull DataSource dataSource,
                      @Nonnull Class<?> daoType,
                      @Nonnull MethodHandle methodHandle )
    {
        super( transactionManager, conversionService, dataSource, daoType, methodHandle, false );

        Update ann = this.methodHandle.getAnnotation( Update.class );
        if( ann == null )
        {
            throw new IllegalArgumentException( "Method '" + methodHandle + "' is not annotated with @Update" );
        }
        this.jdbcTemplate = new JdbcTemplate( conversionService, dataSource, ann.value() );
        this.minRequiredAffectedRows = ann.minAffectedRows() < 0 ? null : ann.minAffectedRows();
        this.maxRequiredAffectedRows = ann.maxAffectedRows() < 0 ? null : ann.maxAffectedRows();
    }

    @Override
    protected Object executeTransactionally( @Nonnull Object proxy, @Nonnull Object... args ) throws SQLException
    {
        Class<?> rawReturnType = this.methodHandle.getReturnType().getRawType();

        UpdateResult result = this.jdbcTemplate.update( createParametersMap( args ),
                                                        this.minRequiredAffectedRows,
                                                        this.maxRequiredAffectedRows );

        if( void.class.equals( rawReturnType ) )
        {
            return null;
        }
        else if( UpdateResult.class.equals( rawReturnType ) )
        {
            return result;
        }
        else if( Number.class.isAssignableFrom( rawReturnType ) )
        {
            return result.getGeneratedKey();
        }
        else if( isPrimitiveInteger( rawReturnType ) )
        {
            Number key = result.getGeneratedKey();
            if( rawReturnType.equals( int.class ) )
            {
                return key.intValue();
            }
            else if( rawReturnType.equals( long.class ) )
            {
                return key.longValue();
            }
            else if( rawReturnType.equals( short.class ) )
            {
                return key.shortValue();
            }
            else if( rawReturnType.equals( byte.class ) )
            {
                return key.byteValue();
            }
            else
            {
                throw new IllegalArgumentException( "Cannot return generated key as '" + rawReturnType.getName() + "' from method '" + this.methodHandle + "'" );
            }
        }
        else
        {
            throw new IllegalStateException( "Unsupported return type for method '" + this.methodHandle + "': " + rawReturnType.getName() );
        }
    }
}
