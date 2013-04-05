package org.mosaic.config.impl;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.lifecycle.annotation.Service;
import org.mosaic.util.reflection.MethodHandle;
import org.mosaic.util.reflection.MethodParameter;
import org.mosaic.util.weaving.MethodInterceptor;
import org.mosaic.util.weaving.MethodInvocation;

/**
 * @author arik
 */
@Service(MethodInterceptor.class)
public class NullabilityMethodInterceptor implements MethodInterceptor
{
    @Nullable
    @Override
    public Object intercept( @Nonnull MethodInvocation invocation ) throws Exception
    {
        // TODO arik: cache @Nonnull indices for each method handle so we don't have to iterate & look for annotations

        Object[] arguments = invocation.getArguments();

        MethodHandle method = invocation.getMethodHandle();

        List<MethodParameter> parameters = method.getParameters();
        for( int i = 0; i < parameters.size(); i++ )
        {
            MethodParameter parameter = parameters.get( i );
            if( parameter.hasAnnotation( Nonnull.class ) )
            {
                Object value = arguments[ i ];
                if( value == null )
                {
                    throw new NullPointerException( "Argument '" + parameter.getName() + "' for method '" + method + "' must not be null" );
                }
            }
        }

        return invocation.proceed( arguments );
    }
}
