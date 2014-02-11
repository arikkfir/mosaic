package org.mosaic.web.server;

import javax.annotation.Nonnull;

/**
 * @author arik
 */
public interface Challanger
{
    void challange( @Nonnull WebInvocation invocation );
}
