package org.mosaic.web.marshall.impl;

import com.google.common.net.MediaType;
import java.io.IOException;
import java.io.OutputStream;
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

    public void marshall( @Nonnull Object value,
                          @Nonnull List<MediaType> allowedMediaTypes,
                          @Nonnull OutputStream outputStream ) throws Exception
    {
        for( MediaType mediaType : allowedMediaTypes )
        {
            for( MessageMarshaller marshaller : this.marshallers )
            {
                if( marshaller.canMarshall( value, mediaType ) )
                {
                    try
                    {
                        marshaller.marshall( value, mediaType, outputStream );
                        return;
                    }
                    finally
                    {
                        try
                        {
                            outputStream.flush();
                        }
                        catch( IOException ignore )
                        {
                        }
                    }
                }
            }
        }
        throw new UnmarshallableContentException( value, allowedMediaTypes );
    }
}
