package org.mosaic.security.realm;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public interface RealmManager
{
    @Nullable
    Realm getRealm( @Nonnull String name );
}
