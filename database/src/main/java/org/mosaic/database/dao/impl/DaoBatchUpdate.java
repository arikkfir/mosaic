package org.mosaic.database.dao.impl;

import javax.annotation.Nonnull;
import javax.sql.DataSource;
import org.mosaic.database.dao.Batch;
import org.mosaic.database.dao.annotation.BatchUpdate;
import org.mosaic.database.tx.TransactionManager;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.util.reflection.MethodHandle;

/**
 * @author arik
 */
public class DaoBatchUpdate extends BaseDaoAction
{
    @Nonnull
    private final JdbcTemplate jdbcTemplate;

    private final int batchSize;

    public DaoBatchUpdate( @Nonnull TransactionManager transactionManager,
                           @Nonnull ConversionService conversionService,
                           @Nonnull DataSource dataSource,
                           @Nonnull Class<?> daoType,
                           @Nonnull MethodHandle methodHandle )
    {
        super( transactionManager, conversionService, dataSource, daoType, methodHandle, false );

        if( !methodHandle.getReturnType().isAssignableFrom( Batch.class ) )
        {
            throw new UnsupportedOperationException( "Method '" + methodHandle + "' has @BatchUpdate annotation, but does not return Batch" );
        }
        else if( !methodHandle.getParameters().isEmpty() )
        {
            throw new UnsupportedOperationException( "Method '" + methodHandle + "' has @BatchUpdate annotation, but has parameters which is not support" );
        }

        BatchUpdate ann = this.methodHandle.getAnnotation( BatchUpdate.class );
        if( ann == null )
        {
            throw new IllegalArgumentException( "Method '" + methodHandle + "' is not annotated with @BatchUpdate" );
        }
        this.jdbcTemplate = new JdbcTemplate( this.conversionService, this.dataSource, ann.value() );
        this.batchSize = ann.batchSize();
    }

    @Override
    public Object execute( @Nonnull Object proxy, @Nonnull Object... args )
    {
        return new BatchImpl( this.transactionManager, this.conversionService, this.methodHandle, this.jdbcTemplate, this.batchSize );
    }

    @Override
    protected Object executeTransactionally( @Nonnull Object proxy, @Nonnull Object... args )
    {
        throw new UnsupportedOperationException();
    }
}
