package org.mosaic.security.realm;

import javax.annotation.Nonnull;

/**
 * @author arik
 */
public interface Realm
{
    @Nonnull
    String getName();

    boolean loadUser( @Nonnull MutableUser user, @Nonnull String userName );
}
