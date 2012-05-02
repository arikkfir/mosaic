package org.mosaic.web.handler;

/**
 * @author arik
 */
public interface InterceptorChain
{
    Object next() throws Exception;
}
