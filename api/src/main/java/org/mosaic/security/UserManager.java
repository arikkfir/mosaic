package org.mosaic.security;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public interface UserManager
{
    @Nullable
    User loadUser( @Nonnull String realmName, @Nonnull String userName );

    @Nonnull
    User getAnonymousUser();

    @Nonnull
    User getUser();

    void setUser( @Nonnull User user );

    void setUserScope( @Nullable UserScope userScope );

    interface UserScope
    {
        @Nullable
        User getUser();

        void setUser( @Nonnull User user );
    }
}
