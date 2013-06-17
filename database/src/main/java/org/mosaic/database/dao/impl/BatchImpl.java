package org.mosaic.database.dao.impl;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.database.dao.Batch;
import org.mosaic.database.dao.BatchUpdateResult;
import org.mosaic.database.tx.TransactionManager;
import org.mosaic.util.collect.LinkedHashMapEx;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.util.reflection.MethodHandle;

/**
 * @author arik
 */
public final class BatchImpl implements Batch
{
    @Nonnull
    private final TransactionManager transactionManager;

    @Nonnull
    private final ConversionService conversionService;

    @Nonnull
    private final MethodHandle methodHandle;

    @Nonnull
    private final JdbcTemplate jdbcTemplate;

    @Nonnull
    private final List<Map<String, Object>> parameterRows = new LinkedList<>();

    private final int batchSize;

    @Nullable
    private Map<String, Object> row;

    public BatchImpl( @Nonnull TransactionManager transactionManager,
                      @Nonnull ConversionService conversionService,
                      @Nonnull MethodHandle methodHandle,
                      @Nonnull JdbcTemplate jdbcTemplate,
                      int batchSize )
    {
        this.transactionManager = transactionManager;
        this.conversionService = conversionService;
        this.methodHandle = methodHandle;
        this.jdbcTemplate = jdbcTemplate;
        this.batchSize = batchSize;
    }

    @Nonnull
    @Override
    public Batch set( @Nonnull String name, @Nullable Object value )
    {
        if( this.row == null )
        {
            this.row = new LinkedHashMapEx<>( 10, this.conversionService );
        }
        this.row.put( name, value );
        return this;
    }

    @Nonnull
    @Override
    public Batch next()
    {
        if( this.row != null )
        {
            this.parameterRows.add( this.row );
            this.row = null;
        }
        return this;
    }

    @Nonnull
    @Override
    public Batch add( @Nonnull Map<String, Object> parameters )
    {
        for( Map.Entry<String, Object> entry : parameters.entrySet() )
        {
            set( entry.getKey(), entry.getValue() );
        }
        return next();
    }

    @Nonnull
    @Override
    public BatchUpdateResult execute() throws SQLException
    {
        this.transactionManager.begin( this.methodHandle.toString(), false );
        try
        {
            BatchUpdateResult result = this.jdbcTemplate.batchUpdate( this.parameterRows, this.batchSize );
            this.transactionManager.apply();
            return result;
        }
        catch( SQLException | RuntimeException e )
        {
            this.transactionManager.fail( e );
            throw e;
        }
    }
}
