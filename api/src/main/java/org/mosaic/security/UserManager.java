package org.mosaic.security;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public interface UserManager
{
    @Nullable
    User loadUser( @Nonnull String userName );

    @Nullable
    User loadUser( @Nonnull String realmName, @Nonnull String userName );

    @Nonnull
    LoginScope getScope();

    @Nonnull
    User getUser();

    @Nonnull
    User login( @Nonnull User user, @Nonnull LoginScope scope );

    void logout();

    enum LoginScope
    {
        REQUEST,
        SESSION,
        REMEMBER_ME
    }
}
