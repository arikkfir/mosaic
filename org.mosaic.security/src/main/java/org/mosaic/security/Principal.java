package org.mosaic.security;

import javax.annotation.Nonnull;

/**
 * @author arik
 */
public interface Principal
{
    @Nonnull
    String getName();
}
