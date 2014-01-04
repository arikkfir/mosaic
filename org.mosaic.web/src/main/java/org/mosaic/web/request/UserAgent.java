package org.mosaic.web.request;

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
