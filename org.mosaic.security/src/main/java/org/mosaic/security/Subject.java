package org.mosaic.security;

import java.util.Collection;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public interface Subject
{
    @Nonnull
    String getName();

    boolean isAuthenticated();

    @Nonnull
    Set<String> getRoles();

    boolean is( @Nonnull Class<? extends Principal> principalType );

    @Nonnull
    TypedCollection<Principal> getPrincipals();

    @Nonnull
    TypedCollection<Object> getCredentials();

    boolean hasPermission( @Nonnull String permission );

    boolean hasPermission( @Nonnull Permission permission );

    void login();

    void logout();

    interface TypedCollection<Type> extends Collection<Type>
    {
        @Nullable
        <T extends Type> T firstOfType( @Nonnull Class<T> type );

        @Nonnull
        <T extends Type> Collection<? extends T> ofType( @Nonnull Class<T> type );
    }
}
