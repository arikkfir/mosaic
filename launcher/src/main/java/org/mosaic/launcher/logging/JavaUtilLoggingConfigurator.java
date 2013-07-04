package org.mosaic.launcher.logging;

import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * @author arik
 */
public class JavaUtilLoggingConfigurator
{
    public final void initializeLogging() throws Exception
    {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }
}
