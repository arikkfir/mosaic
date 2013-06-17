package org.mosaic.database.dao.impl;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.sql.DataSource;
import org.mosaic.database.dao.annotation.Param;
import org.mosaic.database.tx.TransactionManager;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.util.reflection.MethodHandle;
import org.mosaic.util.reflection.MethodParameter;

/**
 * @author arik
 */
public abstract class BaseDaoAction implements DaoAction
{
    private static final List<Class<?>> PRIMITIVE_INT_TYPES = Arrays.<Class<?>>asList( int.class, long.class, short.class, byte.class );

    @Nonnull
    protected final TransactionManager transactionManager;

    @Nonnull
    protected final ConversionService conversionService;

    @Nonnull
    protected final DataSource dataSource;

    @Nonnull
    protected final Class<?> daoType;

    @Nonnull
    protected final MethodHandle methodHandle;

    protected final boolean readOnly;

    @Nonnull
    protected final Map<String, Integer> namedParameterIndices = new HashMap<>( 10 );

    public BaseDaoAction( @Nonnull TransactionManager transactionManager,
                          @Nonnull ConversionService conversionService,
                          @Nonnull DataSource dataSource,
                          @Nonnull Class<?> daoType,
                          @Nonnull MethodHandle methodHandle,
                          boolean readOnly )
    {
        this.transactionManager = transactionManager;
        this.conversionService = conversionService;
        this.dataSource = dataSource;
        this.daoType = daoType;
        this.methodHandle = methodHandle;
        this.readOnly = readOnly;

        // map method parameters to SQL parameters and/or ad-hoc row mapper, callback handler, etc
        for( MethodParameter parameter : methodHandle.getParameters() )
        {
            if( !processParameter( parameter ) )
            {
                throw new IllegalArgumentException( "Parameter '" + parameter + "' could not be processed" );
            }
        }
    }

    @Override
    public Object execute( @Nonnull Object proxy, @Nonnull Object... args ) throws SQLException
    {
        this.transactionManager.begin( this.methodHandle.toString(), this.readOnly );
        try
        {
            Object result = executeTransactionally( proxy, args );
            this.transactionManager.apply();
            return result;
        }
        catch( SQLException | RuntimeException e )
        {
            this.transactionManager.fail( e );
            throw e;
        }
    }

    protected boolean processParameter( @Nonnull MethodParameter parameter )
    {
        Param paramAnnotation = parameter.getAnnotation( Param.class );
        if( paramAnnotation != null )
        {
            this.namedParameterIndices.put( paramAnnotation.value(), parameter.getIndex() );
            return true;
        }
        else
        {
            return false;
        }
    }

    protected final boolean isPrimitiveInteger( Class<?> type )
    {
        return PRIMITIVE_INT_TYPES.contains( type );
    }

    protected final Map<String, Object> createParametersMap( @Nonnull Object[] args )
    {
        Map<String, Object> parameterValues = new HashMap<>( args.length );
        for( Map.Entry<String, Integer> entry : this.namedParameterIndices.entrySet() )
        {
            parameterValues.put( entry.getKey(), args[ entry.getValue() ] );
        }
        return parameterValues;
    }

    protected abstract Object executeTransactionally( @Nonnull Object proxy, @Nonnull Object... args )
            throws SQLException;
}
