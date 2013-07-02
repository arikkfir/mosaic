package org.mosaic.web.handler.impl.adapter;

import java.util.Arrays;
import javax.annotation.Nonnull;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.web.handler.impl.action.RenderPageHandler;
import org.mosaic.web.handler.impl.filter.Filter;
import org.mosaic.web.handler.impl.filter.FindPageFilter;
import org.mosaic.web.handler.impl.filter.HttpMethodFilter;

import static org.mosaic.web.net.HttpMethod.GET;

/**
 * @author arik
 */
public class PageAdapter extends HandlerAdapter
{
    public PageAdapter( @Nonnull ConversionService conversionService )
    {
        super( conversionService,
               -1,
               Integer.MIN_VALUE + 1000,
               new RenderPageHandler(),
               Arrays.<Filter>asList( new HttpMethodFilter( GET ), new FindPageFilter() ) );
    }
}
