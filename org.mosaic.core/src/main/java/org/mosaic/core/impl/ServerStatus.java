package org.mosaic.core.impl;

import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.workflow.Status;

/**
 * @author arik
 */
public final class ServerStatus
{
    @Nonnull
    public static final Status STARTED = new Status( "started" );

    @Nonnull
    public static final Status STOPPED = new Status( "stopped" );

    private ServerStatus()
    {
    }
}
