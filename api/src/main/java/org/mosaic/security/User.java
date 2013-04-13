package org.mosaic.security;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.collect.MapEx;

/**
 * @author arik
 */
public interface User extends MapEx<String, Object>
{
    @Nonnull
    String getName();

    @Nonnull
    Set<String> getRoles();

    boolean is( @Nonnull String type );

    @Nullable
    Principal getPrincipal( @Nonnull String type );

    @Nullable
    <T extends Principal> T getPrincipal( @Nonnull Class<T> type );

    @Nonnull
    <T extends Principal> T requirePrincipal( @Nonnull Class<T> type );

    @Nonnull
    Map<String, Principal> getPrincipals();

    @Nullable
    <T> T getCredential( @Nonnull Class<T> type );

    @Nonnull
    <T> T requireCredential( @Nonnull Class<T> type );

    @Nonnull
    Collection<Object> getCredentials();
}
