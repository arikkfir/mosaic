package org.mosaic.web.handler.impl.adapter;

import java.util.Collection;
import javax.annotation.Nonnull;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.web.handler.impl.action.Handler;
import org.mosaic.web.handler.impl.filter.Filter;

/**
 * @author arik
 */
public class HandlerAdapter extends RequestAdapter
{
    public HandlerAdapter( @Nonnull ConversionService conversionService,
                           long id,
                           int rank,
                           @Nonnull Handler action,
                           @Nonnull Collection<Filter> filters )
    {
        super( conversionService, id, rank, action, filters );
    }
}
