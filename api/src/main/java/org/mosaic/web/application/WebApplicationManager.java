package org.mosaic.web.application;

import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public interface WebApplicationManager
{
    @Nonnull
    Collection<? extends WebApplication> getApplications();

    @Nullable
    WebApplication getApplication( @Nonnull String name );
}
