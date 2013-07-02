package org.mosaic.web.handler.impl.adapter;

import java.util.Collection;
import javax.annotation.Nonnull;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.web.handler.impl.action.Interceptor;
import org.mosaic.web.handler.impl.filter.Filter;

/**
 * @author arik
 */
public class InterceptorAdapter extends RequestAdapter
{
    public InterceptorAdapter( @Nonnull ConversionService conversionService,
                               long id,
                               int rank,
                               @Nonnull Interceptor interceptor,
                               @Nonnull Collection<Filter> filters )
    {
        super( conversionService, id, rank, interceptor, filters );
    }
}
