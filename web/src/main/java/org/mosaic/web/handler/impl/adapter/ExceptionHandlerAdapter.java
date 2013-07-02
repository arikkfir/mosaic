package org.mosaic.web.handler.impl.adapter;

import java.util.Collection;
import javax.annotation.Nonnull;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.web.handler.impl.action.ExceptionHandler;
import org.mosaic.web.handler.impl.filter.Filter;

/**
 * @author arik
 */
public class ExceptionHandlerAdapter extends RequestAdapter
{
    public ExceptionHandlerAdapter( @Nonnull ConversionService conversionService,
                                    long id,
                                    int rank,
                                    @Nonnull ExceptionHandler exceptionHandler,
                                    @Nonnull Collection<Filter> filters )
    {
        super( conversionService, id, rank, exceptionHandler, filters );
    }
}
