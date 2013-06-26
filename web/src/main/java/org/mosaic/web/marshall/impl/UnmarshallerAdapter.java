package org.mosaic.web.marshall.impl;

import com.google.common.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
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
import org.mosaic.web.marshall.annotation.Unmarshaller;
import org.mosaic.web.net.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Arrays.asList;

/**
 * @author arik
 */
public class UnmarshallerAdapter implements Comparable<UnmarshallerAdapter>
{
    private static final Logger LOG = LoggerFactory.getLogger( UnmarshallerAdapter.class );

    private final long id;

    private final int rank;

    @Nonnull
    private final MethodEndpoint endpoint;

    @Nullable
    private final MediaType sourceMediaType;

    @Nullable
    private final TypeToken<?> targetType;

    @Nonnull
    private final List<MethodHandle.ParameterResolver> parameterResolvers = new LinkedList<>();

    @Nullable
    private MethodEndpoint.Invoker endpointInvoker;

    public UnmarshallerAdapter( long id, int rank, @Nonnull MethodEndpoint endpoint )
    {
        this.endpoint = endpoint;
        this.id = id;
        this.rank = rank;

        this.targetType = getEndpoint().getReturnType();

        Unmarshaller unmarshallerAnn = getEndpoint().getAnnotation( Unmarshaller.class );
        if( unmarshallerAnn == null )
        {
            LOG.warn( "Unmarshaller {} has no @Unmarshaller annotation - it will not be activated", getEndpoint() );
            this.sourceMediaType = null;
        }
        else
        {
            MediaType sourceMediaType = new MediaType( unmarshallerAnn.consumes() );
            if( sourceMediaType.hasWildcard() )
            {
                LOG.warn( "Unmarshaller {} has a wildcard in its media type declaration '{}' - it will not be activated", getEndpoint(), sourceMediaType );
                this.sourceMediaType = null;
            }
            else
            {
                this.sourceMediaType = sourceMediaType;
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
                            return UnmarshallerAdapter.this.sourceMediaType;
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
                        if( parameter.getType().isAssignableFrom( TypeToken.class ) )
                        {
                            return resolveContext.require( "targetType", TypeToken.class );
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
                        if( parameter.getType().isAssignableFrom( InputStream.class ) )
                        {
                            return resolveContext.require( "sourceInputStream", InputStream.class );
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
                        if( parameter.getType().isAssignableFrom( Reader.class ) )
                        {
                            return resolveContext.require( "sourceReader", Reader.class );
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

    public boolean canUnmarshall( @Nonnull MediaType sourceMediaType, @Nonnull TypeToken<?> targetType )
    {
        return this.targetType != null
               && this.sourceMediaType != null
               && targetType.isAssignableFrom( this.targetType )
               && sourceMediaType.is( this.sourceMediaType );
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T unmarshall( @Nonnull InputStream sourceInputStream,
                             @Nonnull MediaType sourceMediaType,
                             @Nonnull TypeToken<T> targetType ) throws Exception
    {
        Map<String, Object> resolveContext = new HashMap<>();
        resolveContext.put( "targetType", targetType );

        resolveContext.put( "sourceInputStream", sourceInputStream );

        Charset charset = sourceMediaType.getCharset();
        if( charset == null )
        {
            charset = Charset.forName( "UTF-8" );
        }
        resolveContext.put( "sourceReader", new InputStreamReader( sourceInputStream, charset ) );
        Object value = getEndpointInvoker().resolve( resolveContext ).invoke();
        return ( T ) value;
    }

    @Override
    public int compareTo( UnmarshallerAdapter o )
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
