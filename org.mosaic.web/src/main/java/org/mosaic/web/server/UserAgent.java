package org.mosaic.web.server;

import java.net.InetAddress;
import javax.annotation.Nonnull;

/**
 * @author arik
 */
public interface UserAgent
{
    @Nonnull
    InetAddress getClientAddress();
}
