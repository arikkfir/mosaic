package org.mosaic.web.handler.impl.filter;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import javax.annotation.Nonnull;
import org.mosaic.lifecycle.MethodEndpoint;
import org.mosaic.util.collect.MapEx;
import org.mosaic.web.handler.impl.RequestExecutionPlan;

/**
 * @author arik
 */
public class PathFilter implements Filter
{
    @Nonnull
    private final String[] pathTemplates;

    private final boolean emptyPathListMatchesAll;

    public PathFilter( @Nonnull MethodEndpoint endpoint )
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        this( endpoint, false );
    }

    public PathFilter( @Nonnull MethodEndpoint endpoint, boolean emptyPathListMatchesAll )
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        this.emptyPathListMatchesAll = emptyPathListMatchesAll;
        Annotation endpointType = endpoint.getType();
        Class<? extends Annotation> annotationType = endpointType.annotationType();
        java.lang.reflect.Method method = annotationType.getDeclaredMethod( "value" );
        this.pathTemplates = ( String[] ) method.invoke( endpointType );
    }

    @Override
    public boolean matches( @Nonnull RequestExecutionPlan plan, @Nonnull MapEx<String, Object> context )
    {
        if( this.emptyPathListMatchesAll && this.pathTemplates.length == 0 )
        {
            return true;
        }

        for( String pathTemplate : this.pathTemplates )
        {
            MapEx<String, String> pathParameters = plan.getRequest().getUri().getPathParameters( pathTemplate );
            if( pathParameters != null )
            {
                context.put( "pathTemplate", pathTemplate );
                context.put( "pathParameters", pathParameters );
                return true;
            }
        }
        return false;
    }
}
