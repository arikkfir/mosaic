package org.mosaic.util.method.impl;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.mosaic.core.util.Nonnull;

/**
 * @author arik
 */
final class MethodSignatureKey
{
    @Nonnull
    private final Class<?> declaringClass;

    @Nonnull
    private final String methodName;

    @Nonnull
    private final Class<?>[] argumentTypesArray;

    @Nonnull
    private final List<Class<?>> argumentTypes;

    MethodSignatureKey( @Nonnull Class<?> declaringClass,
                        @Nonnull String methodName,
                        @Nonnull Class<?>... argumentTypes )
    {
        this.declaringClass = declaringClass;
        this.methodName = methodName;
        this.argumentTypesArray = argumentTypes;
        this.argumentTypes = Arrays.asList( argumentTypes );
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals( Object o )
    {
        if( this == o )
        {
            return true;
        }
        if( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        MethodSignatureKey that = ( MethodSignatureKey ) o;
        if( !argumentTypes.equals( that.argumentTypes ) )
        {
            return false;
        }
        else if( !declaringClass.equals( that.declaringClass ) )
        {
            return false;
        }
        else if( !methodName.equals( that.methodName ) )
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    @Override
    public int hashCode()
    {
        int result = declaringClass.hashCode();
        result = 31 * result + methodName.hashCode();
        result = 31 * result + argumentTypes.hashCode();
        return result;
    }

    Method findMethod()
    {
        Class<?> searchType = this.declaringClass;
        while( searchType != null )
        {
            Method[] methods = ( searchType.isInterface() ? searchType.getMethods() : searchType.getDeclaredMethods() );
            for( Method method : methods )
            {
                if( this.methodName.equals( method.getName() ) && Arrays.equals( this.argumentTypesArray, method.getParameterTypes() ) )
                {
                    return method;
                }
            }
            searchType = searchType.getSuperclass();
        }
        return null;
    }
}
