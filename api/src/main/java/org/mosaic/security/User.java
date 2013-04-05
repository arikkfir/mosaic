package org.mosaic.security;

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public interface User extends Map<String, Object>
{
    @Nonnull
    String getName();

    @Nonnull
    Set<String> getRoles();

    @Nonnull
    Set<Principal> getPrincipals();

    @Nonnull
    Map<String, Principal> getPrincipalsMap();

    boolean is( @Nonnull String type );

    @Nullable
    Principal getPrincipal( @Nonnull String type );

    @Nullable
    <T extends Principal> T getPrincipal( @Nonnull Class<T> type );

    @Nonnull
    <T extends Principal> T requirePrincipal( @Nonnull Class<T> type );

    @Nonnull
    List<Object> getCredentials();

    @Nullable
    <T> T getCredential( @Nonnull Class<T> type );

    @Nonnull
    <T> T requireCredential( @Nonnull Class<T> type );
}
