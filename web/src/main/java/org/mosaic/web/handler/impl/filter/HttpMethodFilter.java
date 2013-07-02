package org.mosaic.web.handler.impl.filter;

import java.util.*;
import javax.annotation.Nonnull;
import org.mosaic.util.collect.MapEx;
import org.mosaic.web.handler.impl.RequestExecutionPlan;
import org.mosaic.web.net.HttpMethod;

import static org.mosaic.web.net.HttpMethod.OPTIONS;

/**
 * @author arik
 */
public class HttpMethodFilter implements Filter
{
    @Nonnull
    private final Set<HttpMethod> supportedMethods;

    public HttpMethodFilter( @Nonnull HttpMethod... supportedMethods )
    {
        this( Arrays.asList( supportedMethods ) );
    }

    public HttpMethodFilter( @Nonnull Collection<HttpMethod> supportedMethods )
    {
        this.supportedMethods = Collections.unmodifiableSet( new HashSet<>( supportedMethods ) );
    }

    @Override
    public boolean matches( @Nonnull RequestExecutionPlan plan, @Nonnull MapEx<String, Object> context )
    {
        return plan.getRequest().getMethod() == OPTIONS || this.supportedMethods.contains( plan.getRequest().getMethod() );
    }
}
