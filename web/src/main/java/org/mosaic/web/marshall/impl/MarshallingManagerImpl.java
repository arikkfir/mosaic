package org.mosaic.web.marshall.impl;

import com.google.common.reflect.TypeToken;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentSkipListSet;
import javax.annotation.Nonnull;
import org.mosaic.lifecycle.MethodEndpoint;
import org.mosaic.lifecycle.annotation.*;
import org.mosaic.web.marshall.MarshallingManager;
import org.mosaic.web.marshall.annotation.Marshaller;
import org.mosaic.web.marshall.annotation.Unmarshaller;
import org.mosaic.web.net.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Arrays.asList;

/**
 * @author arik
 */
@Service(MarshallingManager.class)
public class MarshallingManagerImpl implements MarshallingManager
{
    private static final Logger LOG = LoggerFactory.getLogger( MarshallingManagerImpl.class );

    @Nonnull
    private final Collection<MarshallerAdapter> marshallers = new ConcurrentSkipListSet<>();

    @Nonnull
    private final Collection<UnmarshallerAdapter> unmarshallers = new ConcurrentSkipListSet<>();

    @MethodEndpointBind(Marshaller.class)
    public void addMarshaller( @Nonnull MethodEndpoint endpoint, @ServiceId long id, @Rank int rank )
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
    {
        this.marshallers.add( new MarshallerAdapter( id, rank, endpoint ) );
        LOG.debug( "Added @Marshaller {}", endpoint );
    }

    @MethodEndpointUnbind(Marshaller.class)
    public void removeMarshaller( @Nonnull MethodEndpoint endpoint, @ServiceId long id )
    {
        for( Iterator<? extends MarshallerAdapter> iterator = this.marshallers.iterator(); iterator.hasNext(); )
        {
            MarshallerAdapter adapter = iterator.next();
            if( adapter.getId() == id )
            {
                iterator.remove();
                LOG.debug( "Removed @Marshaller {}", endpoint );
                return;
            }
        }
    }

    @MethodEndpointBind(Unmarshaller.class)
    public void addUnmarshaller( @Nonnull MethodEndpoint endpoint, @ServiceId long id, @Rank int rank )
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
    {
        this.unmarshallers.add( new UnmarshallerAdapter( id, rank, endpoint ) );
        LOG.debug( "Added @Unmarshaller {}", endpoint );
    }

    @MethodEndpointUnbind(Unmarshaller.class)
    public void removeUnmarshaller( @Nonnull MethodEndpoint endpoint, @ServiceId long id )
    {
        for( Iterator<? extends UnmarshallerAdapter> iterator = this.unmarshallers.iterator(); iterator.hasNext(); )
        {
            UnmarshallerAdapter adapter = iterator.next();
            if( adapter.getId() == id )
            {
                iterator.remove();
                LOG.debug( "Removed @Unmarshaller {}", endpoint );
                return;
            }
        }
    }

    @Override
    public void marshall( @Nonnull Object value,
                          @Nonnull OutputStream targetOutputStream,
                          @Nonnull MediaType... targetMediaTypes ) throws Exception
    {
        for( MediaType targetMediaType : targetMediaTypes )
        {
            for( MarshallerAdapter adapter : this.marshallers )
            {
                if( adapter.canMarshall( value, targetMediaType ) )
                {
                    adapter.marshall( value, targetMediaType, targetOutputStream );
                    return;
                }
            }
        }
        throw new IllegalStateException( "No suitable marshaller found to marshall '" + value + "' into one of media types '" + asList( targetMediaTypes ) + "'" );
    }

    @Override
    public <T> T unmarshall( @Nonnull InputStream sourceInputStream,
                             @Nonnull MediaType sourceMediaType,
                             @Nonnull TypeToken<? extends T> targetType ) throws Exception
    {
        for( UnmarshallerAdapter adapter : this.unmarshallers )
        {
            if( adapter.canUnmarshall( sourceMediaType, targetType ) )
            {
                return adapter.unmarshall( sourceInputStream, sourceMediaType, targetType );
            }
        }
        throw new IllegalStateException( "No suitable unmarshaller found to unmarshall a '" + targetType + "' from media type '" + sourceMediaType + "'" );
    }
}
