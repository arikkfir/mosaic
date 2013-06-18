package org.mosaic.web.server;

/**
 * @author arik
 */
public interface WebServer
{
    void start() throws Exception;

    void stop() throws Exception;
}
