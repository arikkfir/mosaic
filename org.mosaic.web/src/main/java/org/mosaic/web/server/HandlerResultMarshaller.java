package org.mosaic.web.server;

import com.google.common.net.MediaType;
import javax.annotation.Nonnull;

/**
 * @author arik
 */
public interface HandlerResultMarshaller
{
    boolean canMarshall( @Nonnull MediaType mediaType, @Nonnull Object value );

    void marshall( @Nonnull WebInvocation invocation, @Nonnull Object value ) throws Exception;
}
