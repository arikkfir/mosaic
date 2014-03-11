package org.mosaic.web.server.impl;

import com.google.common.net.MediaType;
import java.util.List;
import javax.annotation.Nonnull;
import org.mosaic.modules.Component;
import org.mosaic.modules.Service;
import org.mosaic.web.server.HandlerResultMarshaller;
import org.mosaic.web.server.UnmarshallableContentException;
import org.mosaic.web.server.WebInvocation;

/**
 * @author arik
 */
@Component
public class MarshallerManager
{
    @Nonnull
    @Service
    private List<HandlerResultMarshaller> marshallers;

    public void marshall( @Nonnull WebInvocation invocation, @Nonnull Object value )
            throws Exception
    {
        for( MediaType mediaType : invocation.getHttpRequest().getAccept() )
        {
            for( HandlerResultMarshaller marshaller : this.marshallers )
            {
                if( marshaller.canMarshall( mediaType, value ) )
                {
                    invocation.getHttpResponse().setContentType( mediaType );
                    marshaller.marshall( invocation, value );
                    return;
                }
            }
        }
        throw new UnmarshallableContentException( value, invocation.getHttpRequest().getAccept() );
    }
}
