package org.mosaic.web.application;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joda.time.Period;
import org.mosaic.util.collections.MapEx;
import org.mosaic.web.security.SecurityConstraint;

/**
 * @author arik
 */
public interface Application
{
    @Nonnull
    String getId();

    @Nonnull
    String getName();

    @Nonnull
    MapEx<String, String> getContext();

    @Nonnull
    MapEx<String, Object> getAttributes();

    @Nonnull
    Set<String> getVirtualHosts();

    @Nonnull
    Period getMaxSessionAge();

    @Nonnull
    ApplicationSecurity getSecurity();

    @Nonnull
    ApplicationResources getResources();

    interface ApplicationSecurity
    {
        @Nonnull
        String getRealmName();

        @Nonnull
        String getPermissionPolicyName();

        @Nullable
        SecurityConstraint getConstraintForPath( @Nonnull String path );
    }

    interface ApplicationResources
    {
        @Nonnull
        Collection<Path> getContentRoots();

        @Nullable
        Resource getResource( @Nonnull String path );
    }
}
