package org.mosaic.web.handler.impl.action;

import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.lifecycle.MethodEndpoint;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.reflection.MethodHandle;

import static java.util.Arrays.asList;

/**
 * @author arik
 */
public abstract class MethodEndpointWrapper
{
    @Nonnull
    protected final MethodEndpoint endpoint;

    @Nonnull
    private final List<MethodHandle.ParameterResolver> parameterResolvers = new LinkedList<>();

    @Nullable
    private MethodEndpoint.Invoker endpointInvoker;

    public MethodEndpointWrapper( @Nonnull MethodEndpoint endpoint )
    {
        this.endpoint = endpoint;
    }

    @Nullable
    protected Object invoke( @Nonnull MapEx<String, Object> context ) throws Exception
    {
        return getEndpointInvoker().resolve( context ).invoke();
    }

    protected synchronized void addParameterResolvers( @Nonnull MethodHandle.ParameterResolver... parameterResolvers )
    {
        if( this.endpointInvoker != null )
        {
            throw new IllegalStateException( "Method endpoint invoker already created!" );
        }
        this.parameterResolvers.addAll( asList( parameterResolvers ) );
    }

    @Nonnull
    private MethodEndpoint.Invoker getEndpointInvoker()
    {
        if( this.endpointInvoker == null )
        {
            synchronized( this )
            {
                if( this.endpointInvoker == null )
                {
                    this.endpointInvoker = this.endpoint.createInvoker( this.parameterResolvers );
                }
            }
        }
        return this.endpointInvoker;
    }
}
