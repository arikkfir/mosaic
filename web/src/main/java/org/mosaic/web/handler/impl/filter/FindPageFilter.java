package org.mosaic.web.handler.impl.filter;

import javax.annotation.Nonnull;
import org.mosaic.util.collect.MapEx;
import org.mosaic.web.application.Page;
import org.mosaic.web.handler.impl.RequestExecutionPlan;
import org.mosaic.web.request.WebRequest;

/**
 * @author arik
 */
public class FindPageFilter implements Filter
{
    @Override
    public boolean matches( @Nonnull RequestExecutionPlan plan, @Nonnull MapEx<String, Object> context )
    {
        WebRequest request = plan.getRequest();
        for( Page page : request.getApplication().getPages() )
        {
            String language = request.getHeaders().getAcceptLanguage().get( 0 ).getLanguage();
            for( String path : page.getPaths( language ) )
            {
                MapEx<String, String> pathParameters = request.getUri().getPathParameters( path );
                if( pathParameters != null )
                {
                    context.put( "page", page );
                    context.put( "pathParameters", pathParameters );
                    return true;
                }
            }
        }
        return false;
    }
}
