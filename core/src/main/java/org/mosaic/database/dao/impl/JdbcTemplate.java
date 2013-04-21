package org.mosaic.database.dao.impl;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.sql.DataSource;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.mosaic.database.dao.*;
import org.mosaic.util.collect.HashMapEx;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.convert.ConversionService;

/**
 * @author arik
 */
public class JdbcTemplate
{
    private static final Pattern JDBC_NAMED_PARAM_PATTERN = Pattern.compile( "\\b:(\\p{javaJavaIdentifierStart}\\p{javaUnicodeIdentifierPart})\\b" );

    @Nonnull
    private final ConversionService conversionService;

    @Nonnull
    private final DataSource dataSource;

    @Nonnull
    private final String baseSql;

    public JdbcTemplate( @Nonnull ConversionService conversionService,
                         @Nonnull DataSource dataSource,
                         @Nonnull String baseSql )
    {
        this.conversionService = conversionService;
        this.dataSource = dataSource;
        this.baseSql = baseSql;
    }

    @Nonnull
    public ConversionService getConversionService()
    {
        return this.conversionService;
    }

    @Nonnull
    public DataSource getDataSource()
    {
        return this.dataSource;
    }

    @Nonnull
    public String getBaseSql()
    {
        return this.baseSql;
    }

    @Nullable
    public <Result> Result execute( @Nonnull StatementProcessor<Result> processor ) throws SQLException
    {
        try( Connection connection = this.dataSource.getConnection() )
        {
            try( PreparedStatement stmt = connection.prepareStatement( this.baseSql.trim() ) )
            {
                return processor.processStatement( stmt );
            }
        }
    }

    @Nullable
    public <Result> Result execute( @Nonnull Map<String, Object> parameters,
                                    @Nonnull StatementProcessor<Result> processor ) throws SQLException
    {
        StringBuilder sql = new StringBuilder( this.baseSql );

        // replace any inline parameters (e.g. "::param1" or "%param1") for use in places JDBC does not allow bind parameters
        // such as LIMIT or GROUP BY clauses
        processInlineParameters( sql, parameters );

        // replace bind parameters such as ":param1" with standard JDBC "?" placeholders, building the final parameters array
        Object[] bindParameters = processBindParameters( sql, parameters );

        // execute
        try( Connection connection = this.dataSource.getConnection() )
        {
            try( PreparedStatement stmt = connection.prepareStatement( sql.toString().trim() ) )
            {
                applyStatementParameters( stmt, bindParameters );
                return processor.processStatement( stmt );
            }
        }
    }

    @Nonnull
    public UpdateResult update( @Nonnull Map<String, Object> parameters,
                                @Nullable final Integer minAffectedRows,
                                @Nullable final Integer maxAffectedRows ) throws SQLException
    {
        //noinspection ConstantConditions
        return execute( parameters, new StatementProcessor<UpdateResult>()
        {
            @Nullable
            @Override
            public UpdateResult processStatement( @Nonnull PreparedStatement stmt ) throws SQLException
            {
                int affectedRows = stmt.executeUpdate();
                if( minAffectedRows != null && affectedRows < minAffectedRows )
                {
                    throw new InsufficientRowsException( affectedRows + " were updated, " + minAffectedRows + " required for successful update" );
                }
                else if( maxAffectedRows != null && affectedRows > maxAffectedRows )
                {
                    throw new TooManyRowsException( affectedRows + " were updated, no more than " + maxAffectedRows + " required for successful update" );
                }

                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if( generatedKeys.next() )
                {
                    return new UpdateResultImpl( affectedRows, generatedKeys.getInt( 1 ) );
                }
                else
                {
                    return new UpdateResultImpl( affectedRows, null );
                }
            }
        } );
    }

    @Nonnull
    public BatchUpdateResult batchUpdate( @Nonnull final List<Map<String, Object>> parameters, final int batchSize )
            throws SQLException
    {
        if( !parameters.isEmpty() )
        {
            StringBuilder sql = new StringBuilder( this.baseSql );

            final List<String> orderedParameterNames = processBindParametersIndices( sql, parameters.get( 0 ).keySet() );

            //noinspection ConstantConditions
            return execute( new StatementProcessor<BatchUpdateResult>()
            {
                @Nullable
                @Override
                public BatchUpdateResult processStatement( @Nonnull PreparedStatement stmt ) throws SQLException
                {
                    List<Integer> affectedRowsByBatch = new LinkedList<>();
                    for( int rowIndex = 0; rowIndex < parameters.size(); rowIndex++ )
                    {
                        Map<String, Object> row = parameters.get( rowIndex );
                        Object[] parameterValues = new Object[ orderedParameterNames.size() ];
                        for( int parameterIndex = 0; parameterIndex < orderedParameterNames.size(); parameterIndex++ )
                        {
                            parameterValues[ parameterIndex ] = row.get( orderedParameterNames.get( parameterIndex ) );
                        }

                        stmt.clearParameters();
                        applyStatementParameters( stmt, parameterValues );
                        stmt.addBatch();

                        if( rowIndex % batchSize == 0 )
                        {
                            int[] affectedRowsForBatch = stmt.executeBatch();
                            int affectedRows = 0;
                            for( int rowCount : affectedRowsForBatch )
                            {
                                affectedRows += rowCount;
                            }
                            affectedRowsByBatch.add( affectedRows );
                        }
                    }

                    if( parameters.size() % batchSize != 0 )
                    {
                        int[] affectedRowsForBatch = stmt.executeBatch();
                        int affectedRows = 0;
                        for( int rowCount : affectedRowsForBatch )
                        {
                            affectedRows += rowCount;
                        }
                        affectedRowsByBatch.add( affectedRows );
                    }

                    return new BatchUpdateResultImpl( affectedRowsByBatch );
                }
            } );
        }
        else
        {
            return new BatchUpdateResultImpl( Collections.<Integer>emptyList() );
        }
    }

    public void queryAndProcess( @Nonnull Map<String, Object> parameters,
                                 @Nonnull final RowCallback rowCallback ) throws SQLException
    {
        query( parameters, new ResultSetProcessor<Object>()
        {
            @Nullable
            @Override
            public Object processResultSet( @Nonnull ResultSet rs ) throws SQLException
            {
                ResultSetMapEx row = new ResultSetMapEx( rs );
                while( rs.next() )
                {
                    row.repopulate();
                    rowCallback.process( row );
                }
                return null;
            }
        } );
    }

    @Nullable
    public <Result> Result query( @Nonnull Map<String, Object> parameters,
                                  @Nonnull final ResultSetProcessor<Result> processor ) throws SQLException
    {
        return execute( parameters, new StatementProcessor<Result>()
        {
            @Nullable
            @Override
            public Result processStatement( @Nonnull PreparedStatement stmt ) throws SQLException
            {
                try( ResultSet rs = stmt.executeQuery() ) //NOPMD - the ResultSetProcessor handles this
                {
                    return processor.processResultSet( rs );
                }
            }
        } );
    }

    @Nonnull
    public <Result> List<Result> queryForList( @Nonnull Map<String, Object> parameters,
                                               @Nonnull final RowMapper<Result> rowMapper ) throws SQLException
    {
        //noinspection ConstantConditions
        return query( parameters, new ResultSetProcessor<List<Result>>()
        {
            @Nullable
            @Override
            public List<Result> processResultSet( @Nonnull ResultSet rs ) throws SQLException
            {
                ResultSetMapEx row = new ResultSetMapEx( rs );

                List<Result> results = new LinkedList<>();
                while( rs.next() )
                {
                    row.repopulate();
                    results.add( rowMapper.map( row ) );
                }
                return results;
            }
        } );
    }

    @Nonnull
    public List<MapEx<String, Object>> queryForListOfMaps( @Nonnull Map<String, Object> parameters ) throws SQLException
    {
        //noinspection ConstantConditions
        return query( parameters, new ResultSetProcessor<List<MapEx<String, Object>>>()
        {
            @Nullable
            @Override
            public List<MapEx<String, Object>> processResultSet( @Nonnull ResultSet rs ) throws SQLException
            {
                ResultSetMapEx row = new ResultSetMapEx( rs );

                List<MapEx<String, Object>> results = new LinkedList<>();
                while( rs.next() )
                {
                    row.repopulate();
                    results.add( new HashMapEx<>( row, conversionService ) );
                }
                return results;
            }
        } );
    }

    @Nonnull
    public <Result> List<Result> queryForListOfObjects( @Nonnull Map<String, Object> parameters,
                                                        @Nonnull final Class<Result> type ) throws SQLException
    {
        //noinspection ConstantConditions
        return query( parameters, new ResultSetProcessor<List<Result>>()
        {
            @Nullable
            @Override
            public List<Result> processResultSet( @Nonnull ResultSet rs ) throws SQLException
            {
                ResultSetMapEx row = new ResultSetMapEx( rs );

                List<Result> results = new LinkedList<>();
                while( rs.next() )
                {
                    row.repopulate();
                    results.add( rs.getObject( 1, type ) );
                }
                return results;
            }
        } );
    }

    @Nullable
    public MapEx<String, Object> queryForMap( @Nonnull Map<String, Object> parameters ) throws SQLException
    {
        return query( parameters, new ResultSetProcessor<MapEx<String, Object>>()
        {
            @Nullable
            @Override
            public MapEx<String, Object> processResultSet( @Nonnull ResultSet rs ) throws SQLException
            {
                if( rs.next() )
                {
                    // we have at least one row - good. Check if we have another row - and if so fail (too-many-rows)
                    if( rs.next() )
                    {
                        // more than one row - fail!
                        throw new TooManyRowsException( "Single row expected" );
                    }
                    else
                    {
                        ResultSetMapEx row = new ResultSetMapEx( rs );
                        row.repopulate();
                        return row;
                    }
                }
                else
                {
                    return null;
                }
            }
        } );
    }

    @Nullable
    public <Result> Result queryForObject( @Nonnull Map<String, Object> parameters,
                                           @Nonnull final RowMapper<Result> rowMapper ) throws SQLException
    {
        return query( parameters, new ResultSetProcessor<Result>()
        {
            @Nullable
            @Override
            public Result processResultSet( @Nonnull ResultSet rs ) throws SQLException
            {
                if( rs.next() )
                {
                    // we have at least one row - good. Check if we have another row - and if so fail (too-many-rows)
                    if( rs.next() )
                    {
                        // more than one row - fail!
                        throw new TooManyRowsException( "Single row expected" );
                    }
                    else
                    {
                        return rowMapper.map( new ResultSetMapEx( rs ) );
                    }
                }
                else
                {
                    return null;
                }
            }
        } );
    }

    @Nullable
    public <Result> Result queryForObject( @Nonnull Map<String, Object> parameters,
                                           @Nonnull final Class<Result> type ) throws SQLException
    {
        return query( parameters, new ResultSetProcessor<Result>()
        {
            @Nullable
            @Override
            public Result processResultSet( @Nonnull ResultSet rs ) throws SQLException
            {
                if( rs.next() )
                {
                    // we have at least one row - good. Check if we have another row - and if so fail (too-many-rows)
                    if( rs.next() )
                    {
                        // more than one row - fail!
                        throw new TooManyRowsException( "Single row expected" );
                    }
                    else
                    {
                        return rs.getObject( 1, type );
                    }
                }
                else
                {
                    return null;
                }
            }
        } );
    }

    @Nonnull
    private List<String> processBindParametersIndices( @Nonnull StringBuilder sql, @Nonnull Set<String> parameterNames )
    {
        List<String> parameters = new ArrayList<>( 10 );
        Matcher matcher = JDBC_NAMED_PARAM_PATTERN.matcher( sql );
        while( matcher.matches() )
        {
            String parameterName = matcher.group( 1 );
            if( parameterNames.contains( parameterName ) )
            {
                parameters.add( parameterName );
                sql.replace( matcher.start(), matcher.end(), "?" );
            }
            matcher = JDBC_NAMED_PARAM_PATTERN.matcher( sql );
        }
        return parameters;
    }

    @Nonnull
    private Object[] processBindParameters( @Nonnull StringBuilder sql, @Nonnull Map<String, Object> parameterValues )
    {
        List<Object> parameters = new ArrayList<>( 10 );
        Matcher matcher = JDBC_NAMED_PARAM_PATTERN.matcher( sql );
        while( matcher.matches() )
        {
            String parameterName = matcher.group( 1 );
            if( parameterValues.containsKey( parameterName ) )
            {
                parameters.add( parameterValues.get( parameterName ) );
                sql.replace( matcher.start(), matcher.end(), "?" );
            }
            matcher = JDBC_NAMED_PARAM_PATTERN.matcher( sql );
        }
        return parameters.toArray();
    }

    private void processInlineParameters( @Nonnull StringBuilder sql, @Nonnull Map<String, Object> parameters )
    {
        for( Map.Entry<String, Object> entry : parameters.entrySet() )
        {
            String usagePattern;
            int location;

            usagePattern = "::" + entry.getKey();
            location = sql.indexOf( usagePattern );
            while( location >= 0 )
            {
                sql.replace( location, location + usagePattern.length(), Objects.toString( entry.getValue(), "null" ) );
                location = sql.indexOf( usagePattern, location + 1 );
            }

            usagePattern = "%" + entry.getKey();
            location = sql.indexOf( usagePattern );
            while( location >= 0 )
            {
                sql.replace( location, location + usagePattern.length(), Objects.toString( entry.getValue(), "null" ) );
                location = sql.indexOf( usagePattern, location + 1 );
            }
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void applyStatementParameters( @Nonnull PreparedStatement stmt, @Nonnull Object[] jdbcParameters )
            throws SQLException
    {
        for( int i = 0; i < jdbcParameters.length; i++ )
        {
            int index = i + 1;
            Object value = jdbcParameters[ i ];

            if( value == null )
            {
                stmt.setObject( index, null );
            }
            else if( value instanceof Boolean )
            {
                stmt.setBoolean( index, ( Boolean ) value );
            }
            else if( value instanceof Byte )
            {
                stmt.setByte( index, ( Byte ) value );
            }
            else if( value instanceof Short )
            {
                stmt.setShort( index, ( Short ) value );
            }
            else if( value instanceof Integer )
            {
                stmt.setInt( index, ( Integer ) value );
            }
            else if( value instanceof Long )
            {
                stmt.setLong( index, ( Long ) value );
            }
            else if( value instanceof Float )
            {
                stmt.setFloat( index, ( Float ) value );
            }
            else if( value instanceof Double )
            {
                stmt.setDouble( index, ( Double ) value );
            }
            else if( value instanceof BigDecimal )
            {
                stmt.setBigDecimal( index, ( BigDecimal ) value );
            }
            else if( value instanceof CharSequence )
            {
                stmt.setString( index, value.toString() );
            }
            else if( value instanceof byte[] )
            {
                stmt.setBytes( index, ( byte[] ) value );
            }
            else if( value instanceof Timestamp )
            {
                stmt.setTimestamp( index, ( Timestamp ) value );
            }
            else if( value instanceof Time )
            {
                stmt.setTime( index, ( Time ) value );
            }
            else if( value instanceof java.sql.Date ) // NOPMD - using fully qualified class name for clarity
            {
                stmt.setDate( index, ( java.sql.Date ) value ); // NOPMD - using fully qualified class name for clarity
            }
            else if( value instanceof java.util.Date ) // NOPMD - using fully qualified class name for clarity
            {
                java.util.Date date = ( java.util.Date ) value; // NOPMD - using fully qualified class name for clarity
                stmt.setTimestamp( index, new Timestamp( date.getTime() ) );
            }
            else if( value instanceof DateTime )
            {
                DateTime dateTime = ( DateTime ) value;
                stmt.setTimestamp( index, new Timestamp( dateTime.getMillis() ) );
            }
            else if( value instanceof LocalDate )
            {
                LocalDate localDate = ( LocalDate ) value;
                java.util.Date judate = localDate.toDate(); // NOPMD - using fully qualified class name for clarity
                stmt.setDate( index, new java.sql.Date( judate.getTime() ) ); // NOPMD - using fully qualified class name for clarity
            }
            else if( value instanceof LocalDateTime )
            {
                LocalDateTime localDate = ( LocalDateTime ) value;
                java.util.Date judate = localDate.toDate(); // NOPMD - using fully qualified class name for clarity
                stmt.setTimestamp( index, new Timestamp( judate.getTime() ) );
            }
            else
            {
                Class<?> type = value.getClass();
                throw new UnsupportedDataTypeException( "Unsupported data type '" + type.getName() + "'", type );
            }
        }
    }

    public static interface StatementProcessor<Result>
    {
        @Nullable
        Result processStatement( @Nonnull PreparedStatement stmt ) throws SQLException;
    }

    public static interface ResultSetProcessor<Result>
    {
        @Nullable
        Result processResultSet( @Nonnull ResultSet rs ) throws SQLException;
    }

    private class ResultSetMapEx extends HashMapEx<String, Object>
    {
        @Nonnull
        private final ResultSet rs;

        @Nonnull
        private final List<String> columnNames = new LinkedList<>();

        private ResultSetMapEx( @Nonnull ResultSet rs ) throws SQLException
        {
            super( 50, JdbcTemplate.this.conversionService );
            this.rs = rs;

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            for( int i = 1; i <= columnCount; i++ )
            {
                this.columnNames.add( metaData.getColumnLabel( i ) );
            }
        }

        private void repopulate() throws SQLException
        {
            this.clear();
            for( String columnName : this.columnNames )
            {
                Object value = this.rs.getObject( columnName );
                this.put( columnName, value );
            }
        }
    }

    private class UpdateResultImpl implements UpdateResult
    {
        private final int affectedRowsCount;

        private final Number generatedKey;

        private UpdateResultImpl( int affectedRowsCount, Number generatedKey )
        {
            this.affectedRowsCount = affectedRowsCount;
            this.generatedKey = generatedKey;
        }

        @Override
        public int getAffectedRowsCount()
        {
            return this.affectedRowsCount;
        }

        @Override
        public Number getGeneratedKey()
        {
            return this.generatedKey;
        }
    }

    private class BatchUpdateResultImpl implements BatchUpdateResult
    {
        @Nonnull
        private final List<Integer> affectedRowsCountByBatch;

        private final int affectedRowsCount;

        private BatchUpdateResultImpl( @Nonnull List<Integer> affectedRowsCountByBatch )
        {
            int affectedRowsCount = 0;
            for( int rowCount : affectedRowsCountByBatch )
            {
                affectedRowsCount += rowCount;
            }
            this.affectedRowsCount = affectedRowsCount;
            this.affectedRowsCountByBatch = affectedRowsCountByBatch;
        }

        @Override
        public int getAffectedRowsCount()
        {
            return this.affectedRowsCount;
        }

        @Nonnull
        @Override
        public List<Integer> getAffectedRowsCountByBatch()
        {
            return this.affectedRowsCountByBatch;
        }
    }
}
