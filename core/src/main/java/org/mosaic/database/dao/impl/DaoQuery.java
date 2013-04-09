package org.mosaic.database.dao.impl;

import com.google.common.primitives.Primitives;
import com.google.common.reflect.TypeToken;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.sql.DataSource;
import org.mosaic.database.dao.InsufficientRowsException;
import org.mosaic.database.dao.RowCallback;
import org.mosaic.database.dao.RowMapper;
import org.mosaic.database.dao.RowMapperCreationException;
import org.mosaic.database.dao.annotation.Limit;
import org.mosaic.database.dao.annotation.Query;
import org.mosaic.database.dao.annotation.RowMapperBean;
import org.mosaic.database.dao.annotation.RowMapperType;
import org.mosaic.database.tx.TransactionManager;
import org.mosaic.lifecycle.Module;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.util.reflection.MethodHandle;
import org.mosaic.util.reflection.MethodParameter;

/**
 * @author arik
 */
public class DaoQuery extends BaseDaoAction
{
    @Nonnull
    private final Module module;

    @Nonnull
    private final JdbcTemplate jdbcTemplate;

    @Nullable
    private RowMapperFactory rowMapperFactory;

    @Nullable
    private Integer rowCallbackParameterIndex;

    public DaoQuery( @Nonnull TransactionManager transactionManager,
                     @Nonnull ConversionService conversionService,
                     @Nonnull DataSource dataSource,
                     @Nonnull Module module,
                     @Nonnull Class<?> daoType,
                     @Nonnull MethodHandle methodHandle )
    {
        super( transactionManager, conversionService, dataSource, daoType, methodHandle, true );
        this.module = module;

        Query queryAnn = this.methodHandle.getAnnotation( Query.class );
        if( queryAnn == null )
        {
            throw new IllegalArgumentException( "Method '" + methodHandle + "' is not annotated with @Query" );
        }
        String sql = queryAnn.value();

        // discover limit to set on query
        Limit limitAnn = methodHandle.getAnnotation( Limit.class );
        if( limitAnn != null )
        {
            sql += " LIMIT " + limitAnn.value();
        }

        this.jdbcTemplate = new JdbcTemplate( conversionService, dataSource, sql );

        // create the row-mapper factory (either a bean in the DAO's module or a class which is instantiated)
        if( methodHandle.hasAnnotation( RowMapperBean.class ) )
        {
            //noinspection ConstantConditions
            this.rowMapperFactory = new RowMapperFromBeanFactory( methodHandle.getAnnotation( RowMapperBean.class ).value() );
        }
        else if( methodHandle.hasAnnotation( RowMapperType.class ) )
        {
            RowMapperType ann = methodHandle.getAnnotation( RowMapperType.class );
            //noinspection ConstantConditions
            this.rowMapperFactory = new RowMapperFromClassFactory( ann.value(), ann.shareInstance() );
        }
    }

    @Override
    protected boolean processParameter( @Nonnull MethodParameter parameter )
    {
        if( super.processParameter( parameter ) )
        {
            return true;
        }

        Class<?> rawType = parameter.getType().getRawType();
        if( RowMapper.class.isAssignableFrom( rawType ) )
        {
            this.rowMapperFactory = new RowMapperFromParameterFactory( parameter.getIndex() );
            return true;
        }
        else if( RowCallback.class.isAssignableFrom( rawType ) )
        {
            this.rowCallbackParameterIndex = parameter.getIndex();
            return true;
        }
        else
        {
            return false;
        }
    }

    @Override
    protected Object executeTransactionally( @Nonnull Object proxy, @Nonnull Object... args )
            throws SQLException
    {
        Class<?> rawReturnType = this.methodHandle.getReturnType().getRawType();

        Map<String, Object> parameters = createParametersMap( args );
        if( void.class.equals( rawReturnType ) )
        {
            if( this.rowCallbackParameterIndex != null )
            {
                this.jdbcTemplate.queryAndProcess( parameters, ( RowCallback ) args[ this.rowCallbackParameterIndex ] );
                return null;
            }
            else
            {
                throw new IllegalStateException( "Method '" + this.methodHandle + "' does not return a value and does not accept a RowCallback (useless query?)" );
            }
        }
        else if( Number.class.isAssignableFrom( rawReturnType ) )
        {
            return this.jdbcTemplate.queryForObject( parameters, rawReturnType );
        }
        else if( isPrimitiveInteger( rawReturnType ) )
        {
            Object value = this.jdbcTemplate.queryForObject( parameters, Primitives.wrap( rawReturnType ) );
            if( value == null )
            {
                throw new InsufficientRowsException( "Method '" + this.methodHandle + "' returns a primitive number, but no row was fetched" );
            }
            else
            {
                return value;
            }
        }
        else if( Map.class.isAssignableFrom( rawReturnType ) )
        {
            return this.jdbcTemplate.queryForMap( parameters );
        }
        else if( List.class.equals( rawReturnType ) )
        {
            if( this.rowMapperFactory != null )
            {
                return this.jdbcTemplate.queryForList( parameters, this.rowMapperFactory.getRowMapper( args ) );
            }
            else
            {
                Type listGetReturnTypeToken;
                try
                {
                    listGetReturnTypeToken = List.class.getMethod( "get", int.class ).getGenericReturnType();
                }
                catch( NoSuchMethodException e )
                {
                    throw new IllegalStateException( "Could not discover type of list from method '" + this.methodHandle + "': " + e.getMessage(), e );
                }
                TypeToken<?> listItemTypeToken = this.methodHandle.getReturnType().resolveType( listGetReturnTypeToken );
                if( listItemTypeToken.getRawType().isAssignableFrom( MapEx.class ) )
                {
                    return this.jdbcTemplate.queryForListOfMaps( parameters );
                }
                else
                {
                    return this.jdbcTemplate.queryForListOfObjects( parameters, listItemTypeToken.getRawType() );
                }
            }
        }
        else
        {
            return this.jdbcTemplate.queryForObject( parameters, rawReturnType );
        }
    }

    private interface RowMapperFactory
    {
        @Nonnull
        RowMapper<?> getRowMapper( @Nonnull Object[] args );
    }

    private class RowMapperFromBeanFactory implements RowMapperFactory
    {
        @Nonnull
        private final String beanName;

        public RowMapperFromBeanFactory( @Nonnull String beanName )
        {
            this.beanName = beanName;
        }

        @Nonnull
        @Override
        public RowMapper<?> getRowMapper( @Nonnull Object[] args )
        {
            try
            {
                return module.getBean( this.beanName, RowMapper.class );
            }
            catch( Exception e )
            {
                throw new RowMapperCreationException( "Could not obtain row mapper bean '" + this.beanName + "' from module '" + module + "': " + e.getMessage(), e );
            }
        }
    }

    private class RowMapperFromParameterFactory implements RowMapperFactory
    {
        private final int parameterIndex;

        public RowMapperFromParameterFactory( int parameterIndex )
        {
            this.parameterIndex = parameterIndex;
        }

        @Nonnull
        @Override
        public RowMapper<?> getRowMapper( @Nonnull Object[] args )
        {
            Object rowMapper = args[ this.parameterIndex ];
            if( rowMapper instanceof RowMapper )
            {
                return ( RowMapper ) rowMapper;
            }
            else
            {
                throw new RowMapperCreationException( "Parameter " + this.parameterIndex + " is not a row mapper (" + rowMapper + ")" );
            }
        }
    }

    private class RowMapperFromClassFactory implements RowMapperFactory
    {
        @Nonnull
        private final Class<? extends RowMapper> rowMapperType;

        private final boolean shared;

        @Nullable
        private RowMapper<?> instance;

        public RowMapperFromClassFactory( @Nonnull Class<? extends RowMapper> rowMapperType, boolean shared )
        {
            this.rowMapperType = rowMapperType;
            this.shared = shared;
        }

        @Nonnull
        @Override
        public RowMapper<?> getRowMapper( @Nonnull Object[] args )
        {
            try
            {
                if( this.shared )
                {
                    if( this.instance == null )
                    {
                        this.instance = this.rowMapperType.newInstance();
                    }
                    return this.instance;
                }
                else
                {
                    return this.rowMapperType.newInstance();
                }
            }
            catch( Exception e )
            {
                throw new RowMapperCreationException( "Could not instantiate row mapper of type '" + this.rowMapperType.getName() + "': " + e.getMessage(), e );
            }
        }
    }
}
