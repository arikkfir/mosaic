package org.mosaic.web.handler;

import javax.annotation.Nullable;

/**
 * @author arik
 */
public interface InterceptorChain
{
    @Nullable
    Object next() throws Exception;
}
