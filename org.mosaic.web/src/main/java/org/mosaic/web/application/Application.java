package org.mosaic.web.application;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joda.time.Period;
import org.mosaic.util.collections.MapEx;
import org.mosaic.web.server.SecurityConstraint;

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
    String getRealmName();

    @Nonnull
    String getPermissionPolicyName();

    @Nullable
    SecuredPath getConstraintForPath( @Nonnull String path );

    @Nonnull
    Collection<Path> getContentRoots();

    @Nullable
    ApplicationResource getResource( @Nonnull String path );

    interface ApplicationResource
    {
        @Nonnull
        Path getPath();

        boolean isCompressionEnabled();

        boolean isBrowsingEnabled();

        @Nullable
        Period getCachePeriod();
    }

    interface SecuredPath extends SecurityConstraint
    {
        @Nonnull
        String getPath();
    }
}
