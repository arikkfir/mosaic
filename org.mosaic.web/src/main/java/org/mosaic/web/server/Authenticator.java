package org.mosaic.web.server;

import com.google.common.base.Optional;
import javax.annotation.Nonnull;

/**
 * @author arik
 */
public interface Authenticator
{
    @Nonnull
    Optional<Authentication> authenticate( @Nonnull WebInvocation invocation );
}
