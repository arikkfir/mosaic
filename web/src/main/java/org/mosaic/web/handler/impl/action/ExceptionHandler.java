package org.mosaic.web.handler.impl.action;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.collect.MapEx;
import org.mosaic.web.request.WebRequest;

/**
 * @author arik
 */
public interface ExceptionHandler extends Participator
{
    int getDistance( @Nonnull Throwable throwable );

    @Nullable
    Object handle( @Nonnull WebRequest request,
                   @Nonnull Throwable throwable,
                   @Nonnull MapEx<String, Object> context ) throws Exception;
}
