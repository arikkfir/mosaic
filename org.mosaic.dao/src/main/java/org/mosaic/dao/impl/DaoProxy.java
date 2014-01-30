package org.mosaic.dao.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import org.mosaic.dao.DaoException;
import org.mosaic.dao.Query;
import org.mosaic.datasource.TransactionManager;
import org.mosaic.modules.Service;
import org.mosaic.util.method.InvokableMethodHandle;
import org.mosaic.util.method.MethodHandleFactory;

import static java.lang.String.format;

/**
 * @author arik
 */
final class DaoProxy implements InvocationHandler
{
    @Nonnull
    private final Map<Method, Action> methodActions = new HashMap<>();

    @Nonnull
    @Service
    private TransactionManager transactionManager;

    @Nonnull
    @Service
    private MethodHandleFactory methodHandleFactory;

    DaoProxy( @Nonnull Class<?> daoType, @Nonnull String dataSourceName )
    {
        Class<?> type = daoType;
        while( type != null )
        {
            for( Method method : daoType.getDeclaredMethods() )
            {
                if( method.isAnnotationPresent( Query.class ) )
                {
                    InvokableMethodHandle methodHandle = this.methodHandleFactory.findMethodHandle( method );
                    this.methodActions.put( method, new QueryAction( methodHandle, dataSourceName ) );
                }
                else
                {
                    String daoName = type.getSimpleName();
                    String daoMethodName = method.getName();
                    String msg = "method '%s' in @Dao '%s' is not properly annotated";
                    throw new DaoException( format( msg, daoMethodName, daoName ) );
                }
            }
            type = type.getSuperclass();
        }
    }

    @Override
    public Object invoke( Object proxy, Method method, Object[] args ) throws Throwable
    {
        switch( method.getName() )
        {
            case "equals":
                return equals( args[ 0 ] );
            case "hashCode":
                return hashCode();
            default:
                Action action = this.methodActions.get( method );
                if( action != null )
                {
                    return action.execute( args );
                }
                else
                {
                    throw new DaoException( "unknown DAO method: " + method.toGenericString() );
                }
        }
    }
}
