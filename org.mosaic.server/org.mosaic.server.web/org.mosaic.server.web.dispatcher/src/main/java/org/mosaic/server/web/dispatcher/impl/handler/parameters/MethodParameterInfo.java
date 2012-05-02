package org.mosaic.server.web.dispatcher.impl.handler.parameters;

import java.lang.reflect.Method;

/**
 * @author arik
 */
public class MethodParameterInfo
{

    private final Method method;

    private final int index;

    public MethodParameterInfo( Method method, int index )
    {
        this.method = method;
        this.index = index;
    }

    public Method getMethod( )
    {
        return method;
    }

    public int getIndex( )
    {
        return index;
    }
}
