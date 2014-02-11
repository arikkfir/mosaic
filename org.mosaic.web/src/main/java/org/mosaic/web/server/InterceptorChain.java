package org.mosaic.web.server;

import javax.annotation.Nullable;

/**
 * @author arik
 */
public interface InterceptorChain
{
    @Nullable
    Object proceed() throws Throwable;
}
