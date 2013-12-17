package org.mosaic.web.marshall.impl;

import com.google.common.net.MediaType;
import java.util.List;
import javax.annotation.Nonnull;
import org.mosaic.modules.Component;
import org.mosaic.modules.Service;
import org.mosaic.web.marshall.MessageMarshaller;
import org.mosaic.web.marshall.UnmarshallableContentException;

/**
 * @author arik
 */
@Component
public class MarshallerManager
{
    @Nonnull
    @Service
    private List<MessageMarshaller> marshallers;

    public void marshall( @Nonnull MessageMarshaller.MarshallingSink sink,
                          @Nonnull List<MediaType> allowedMediaTypes ) throws Exception
    {
        for( MediaType mediaType : allowedMediaTypes )
        {
            for( MessageMarshaller marshaller : this.marshallers )
            {
                if( marshaller.canMarshall( sink.getValue(), mediaType ) )
                {
                    sink.setContentType( mediaType );
                    marshaller.marshall( sink );
                }
            }
        }
        throw new UnmarshallableContentException( sink.getValue(), allowedMediaTypes );
    }
}
