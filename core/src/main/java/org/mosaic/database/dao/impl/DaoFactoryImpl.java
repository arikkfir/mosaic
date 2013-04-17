package org.mosaic.database.dao.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.PreDestroy;
import javax.sql.DataSource;
import org.mosaic.database.dao.DaoFactory;
import org.mosaic.database.dao.annotation.BatchUpdate;
import org.mosaic.database.dao.annotation.Query;
import org.mosaic.database.dao.annotation.Update;
import org.mosaic.database.tx.TransactionManager;
import org.mosaic.lifecycle.Module;
import org.mosaic.lifecycle.annotation.BeanRef;
import org.mosaic.lifecycle.annotation.ModuleRef;
import org.mosaic.lifecycle.annotation.Service;
import org.mosaic.lifecycle.annotation.ServiceRef;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.util.pair.ImmutableComparablePair;
import org.mosaic.util.pair.Pair;
import org.mosaic.util.reflection.MethodHandle;
import org.mosaic.util.reflection.MethodHandleFactory;

/**
 * @author arik
 */
@Service( DaoFactory.class )
public class DaoFactoryImpl implements DaoFactory
{
    @Nonnull
    private final LoadingCache<Pair<Class<?>, DataSource>, Object> daoCache;

    @Nonnull
    private Module module;

    @Nonnull
    private ConversionService conversionService;

    @Nonnull
    private TransactionManager transactionManager;

    @Nonnull
    private MethodHandleFactory methodHandleFactory;

    public DaoFactoryImpl()
    {
        this.daoCache = CacheBuilder.newBuilder()
                                    .initialCapacity( 1000 )
                                    .concurrencyLevel( 20 )
                                    .build( new CacheLoader<Pair<Class<?>, DataSource>, Object>()
                                    {
                                        @Override
                                        public Object load( Pair<Class<?>, DataSource> key ) throws Exception
                                        {
                                            Class<?> type = key.getKey();
                                            if( type == null )
                                            {
                                                throw new IllegalArgumentException( "Class must not be null" );
                                            }

                                            DataSource dataSource = key.getValue();
                                            if( dataSource == null )
                                            {
                                                throw new IllegalArgumentException( "Data source must not be null" );
                                            }

                                            return Proxy.newProxyInstance(
                                                    type.getClassLoader(),
                                                    new Class<?>[] { type },
                                                    new DaoInvocationHandler( type, dataSource )
                                            );
                                        }
                                    } );
    }

    @ModuleRef
    public void setModule( @Nonnull Module module )
    {
        this.module = module;
    }

    @ServiceRef
    public void setConversionService( @Nonnull ConversionService conversionService )
    {
        this.conversionService = conversionService;
    }

    @BeanRef
    public void setTransactionManager( @Nonnull TransactionManager transactionManager )
    {
        this.transactionManager = transactionManager;
    }

    @ServiceRef
    public void setMethodHandleFactory( @Nonnull MethodHandleFactory methodHandleFactory )
    {
        this.methodHandleFactory = methodHandleFactory;
    }

    @PreDestroy
    public void destroy()
    {
        this.daoCache.invalidateAll();
    }

    @Nonnull
    @Override
    public <T> T create( @Nonnull final Class<T> type, @Nonnull final DataSource dataSource )
    {
        try
        {
            return type.cast( this.daoCache.get( ImmutableComparablePair.<Class<?>, DataSource>of( type, dataSource ) ) );
        }
        catch( Exception e )
        {
            throw new IllegalStateException( "Could not create DAO for '" + type + "': " + e.getMessage(), e );
        }
    }

    private class DaoInvocationHandler implements InvocationHandler
    {
        @Nonnull
        private final Class<?> daoType;

        @Nonnull
        private final DataSource dataSource;

        @Nonnull
        private final Map<Method, DaoAction> methodActions;

        private DaoInvocationHandler( @Nonnull Class<?> daoType, @Nonnull DataSource dataSource )
        {
            this.daoType = daoType;
            this.dataSource = dataSource;

            final Map<Method, DaoAction> actions = new HashMap<>();

            Set<Class<?>> types = new LinkedHashSet<>();

            Class<?> type = daoType;
            while( type != null )
            {
                types.add( type );
                type = type.getSuperclass();
            }
            Collections.addAll( types, daoType.getInterfaces() );

            for( Class<?> clazz : types )
            {
                for( Method method : clazz.getDeclaredMethods() )
                {
                    MethodHandle methodHandle = methodHandleFactory.findMethodHandle( method );
                    if( method.isAnnotationPresent( Query.class ) )
                    {
                        actions.put( method, new DaoQuery( transactionManager, conversionService, DaoInvocationHandler.this.dataSource, module, this.daoType, methodHandle ) );
                    }
                    else if( method.isAnnotationPresent( Update.class ) )
                    {
                        actions.put( method, new DaoUpdate( transactionManager, conversionService, DaoInvocationHandler.this.dataSource, this.daoType, methodHandle ) );
                    }
                    else if( method.isAnnotationPresent( BatchUpdate.class ) )
                    {
                        actions.put( method, new DaoBatchUpdate( transactionManager, conversionService, DaoInvocationHandler.this.dataSource, this.daoType, methodHandle ) );
                    }
                }
            }
            this.methodActions = Collections.unmodifiableMap( actions );
        }

        @Override
        public Object invoke( Object proxy, Method method, Object[] args ) throws Throwable
        {
            if( args == null )
            {
                args = new Object[ 0 ];
            }

            Class<?>[] methodParameterTypes = method.getParameterTypes();
            if( method.getName().equals( "equals" ) && methodParameterTypes.length == 1 && methodParameterTypes[ 0 ].equals( Object.class ) )
            {
                return proxy == args[ 0 ];
            }
            else if( method.getName().equals( "hashCode" ) && methodParameterTypes.length == 0 )
            {
                return System.identityHashCode( proxy );
            }
            else if( method.getName().equals( "toString" ) && methodParameterTypes.length == 0 )
            {
                return this.daoType.getSimpleName() + "@" + System.identityHashCode( proxy );
            }
            else
            {
                DaoAction action = this.methodActions.get( method );
                if( action != null )
                {
                    return action.execute( proxy, args );
                }
                else
                {
                    throw new UnsupportedOperationException( "Non-DAO method in " + this.daoType.getName() + ": " + method.toGenericString() );
                }
            }
        }
    }
}
