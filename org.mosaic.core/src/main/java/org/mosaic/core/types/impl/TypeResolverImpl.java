package org.mosaic.core.types.impl;

import com.fasterxml.classmate.ResolvedType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.mosaic.core.types.TypeHandle;
import org.mosaic.core.types.TypeResolver;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.concurrency.ReadWriteLock;

/**
 * @author arik
 */
public class TypeResolverImpl implements TypeResolver
{
    @Nonnull
    private final ReadWriteLock lock;

    @Nonnull
    private final Map<Type, TypeHandle> typesCache = new WeakHashMap<>( 100000 );

    public TypeResolverImpl( @Nonnull ReadWriteLock lock )
    {
        this.lock = lock;
    }

    @Nonnull
    @Override
    public TypeHandle getTypeHandle( @Nonnull Type type )
    {
        return this.lock.read( () -> {

            TypeHandle handle = this.typesCache.get( type );
            if( handle == null )
            {
                this.lock.releaseReadLock();
                this.lock.acquireWriteLock();
                try
                {
                    handle = this.typesCache.get( type );
                    if( handle == null )
                    {
                        com.fasterxml.classmate.TypeResolver typeResolver = new com.fasterxml.classmate.TypeResolver();
                        ResolvedType resolvedType = typeResolver.resolve( type );
                        handle = new TypeHandleImpl( resolvedType );
                        this.typesCache.put( type, handle );
                    }
                }
                finally
                {
                    this.lock.releaseWriteLock();
                    this.lock.acquireReadLock();
                }
            }
            return handle;
        } );
    }

    @Nonnull
    @Override
    public TypeHandle getTypeHandle( @Nonnull Field field )
    {
        return getTypeHandle( field.getGenericType() );
    }

    @Nonnull
    @Override
    public TypeHandle getReturnTypeHandle( @Nonnull Method method )
    {
        return getTypeHandle( method.getGenericReturnType() );
    }

    @Nonnull
    @Override
    public List<TypeHandle> getParametersTypeHandles( @Nonnull Constructor<?> constructor )
    {
        return getTypeHandles( constructor.getGenericParameterTypes() );
    }

    @Nonnull
    @Override
    public List<TypeHandle> getParametersTypeHandles( @Nonnull Method method )
    {
        return getTypeHandles( method.getGenericParameterTypes() );
    }

    @Nonnull
    private List<TypeHandle> getTypeHandles( @Nonnull Type[] types )
    {
        List<TypeHandle> typeHandles = new LinkedList<>();
        for( Type type : types )
        {
            typeHandles.add( getTypeHandle( type ) );
        }
        return typeHandles;
    }

    private class TypeHandleImpl implements TypeHandle
    {
        @Nonnull
        private final ResolvedType resolvedType;

        private TypeHandleImpl( @Nonnull ResolvedType resolvedType )
        {
            this.resolvedType = resolvedType;
        }
    }
}
