package org.mosaic.web.marshall.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.lifecycle.MethodEndpoint;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.reflection.MethodHandle;
import org.mosaic.util.reflection.MethodParameter;
import org.mosaic.web.marshall.annotation.Marshaller;
import org.mosaic.web.marshall.annotation.Value;
import org.mosaic.web.net.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Arrays.asList;

/**
 * @author arik
 */
public class MarshallerAdapter implements Comparable<MarshallerAdapter>
{
    private static final Logger LOG = LoggerFactory.getLogger( MarshallerAdapter.class );

    private final long id;

    private final int rank;

    @Nonnull
    private final MethodEndpoint endpoint;

    @Nullable
    private final MediaType targetMediaType;

    @Nullable
    private final Class<?> marshallableType;

    @Nonnull
    private final List<MethodHandle.ParameterResolver> parameterResolvers = new LinkedList<>();

    @Nullable
    private MethodEndpoint.Invoker endpointInvoker;

    public MarshallerAdapter( long id, int rank, @Nonnull MethodEndpoint endpoint )
    {
        this.endpoint = endpoint;
        this.id = id;
        this.rank = rank;

        Class<?> marshallableType = null;
        for( MethodParameter parameter : getEndpoint().getParameters() )
        {
            if( parameter.hasAnnotation( Value.class ) )
            {
                marshallableType = parameter.getType().getRawType();
                break;
            }
        }

        if( marshallableType == null )
        {
            LOG.warn( "Marshaller {} has no @Value parameter - it will not be activated", getEndpoint() );
            this.marshallableType = null;
        }
        else
        {
            this.marshallableType = marshallableType;
        }

        Marshaller marshallerAnn = getEndpoint().getAnnotation( Marshaller.class );
        if( marshallerAnn == null )
        {
            LOG.warn( "Marshaller {} has no @Marshaller annotation - it will not be activated", getEndpoint() );
            this.targetMediaType = null;
        }
        else
        {
            MediaType targetMediaType = new MediaType( marshallerAnn.produces() );
            if( targetMediaType.hasWildcard() )
            {
                LOG.warn( "Marshaller {} has a wildcard in its media type declaration '{}' - it will not be activated", getEndpoint(), targetMediaType );
                this.targetMediaType = null;
            }
            else
            {
                this.targetMediaType = targetMediaType;
            }
        }

        addParameterResolvers(
                new MethodHandle.ParameterResolver()
                {
                    @Nullable
                    @Override
                    public Object resolve( @Nonnull MethodParameter parameter,
                                           @Nonnull MapEx<String, Object> resolveContext ) throws IOException
                    {
                        if( parameter.getType().isAssignableFrom( MediaType.class ) )
                        {
                            return MarshallerAdapter.this.targetMediaType;
                        }
                        return SKIP;
                    }
                },
                new MethodHandle.ParameterResolver()
                {
                    @Nullable
                    @Override
                    public Object resolve( @Nonnull MethodParameter parameter,
                                           @Nonnull MapEx<String, Object> resolveContext ) throws IOException
                    {
                        if( parameter.getType().isAssignableFrom( OutputStream.class ) )
                        {
                            return resolveContext.require( "targetOutputStream", OutputStream.class );
                        }
                        return SKIP;
                    }
                },
                new MethodHandle.ParameterResolver()
                {
                    @Nullable
                    @Override
                    public Object resolve( @Nonnull MethodParameter parameter,
                                           @Nonnull MapEx<String, Object> resolveContext ) throws IOException
                    {
                        if( parameter.getType().isAssignableFrom( Writer.class ) )
                        {
                            return resolveContext.require( "targetWriter", Writer.class );
                        }
                        return SKIP;
                    }
                },
                new MethodHandle.ParameterResolver()
                {
                    @Nullable
                    @Override
                    public Object resolve( @Nonnull MethodParameter parameter,
                                           @Nonnull MapEx<String, Object> resolveContext ) throws IOException
                    {
                        if( parameter.hasAnnotation( Value.class ) )
                        {
                            return resolveContext.require( "value" );
                        }
                        return SKIP;
                    }
                }
        );
    }

    public long getId()
    {
        return this.id;
    }

    @Nonnull
    public MethodEndpoint getEndpoint()
    {
        return this.endpoint;
    }

    public boolean canMarshall( @Nonnull Object value, @Nonnull MediaType targetMediaType )
    {
        return this.marshallableType != null
               && this.targetMediaType != null
               && this.marshallableType.isInstance( value )
               && this.targetMediaType.is( targetMediaType );
    }

    public void marshall( @Nonnull Object value,
                          @Nonnull MediaType targetMediaType,
                          @Nonnull OutputStream targetOutputStream ) throws Exception
    {
        Map<String, Object> resolveContext = new HashMap<>();
        resolveContext.put( "value", value );

        resolveContext.put( "targetOutputStream", targetOutputStream );

        Charset charset = targetMediaType.getCharset();
        if( charset == null )
        {
            charset = Charset.forName( "UTF-8" );
        }
        resolveContext.put( "targetWriter", new OutputStreamWriter( targetOutputStream, charset ) );

        getEndpointInvoker().resolve( resolveContext ).invoke();
    }

    @Override
    public int compareTo( MarshallerAdapter o )
    {
        if( this.rank > o.rank )
        {
            return -1;
        }
        else if( this.rank < o.rank )
        {
            return 1;
        }
        else if( this.id < o.id )
        {
            return -1;
        }
        else if( this.id > o.id )
        {
            return 1;
        }
        else
        {
            return 0;
        }
    }

    private void addParameterResolvers( @Nonnull MethodHandle.ParameterResolver... parameterResolvers )
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
