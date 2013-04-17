package org.mosaic.security.policy;

import java.util.List;
import javax.annotation.Nonnull;

/**
 * @author arik
 */
public interface Permission
{
    boolean implies( @Nonnull String childPermissionString );

    boolean implies( @Nonnull Permission childPermission );

    boolean impliedBy( @Nonnull String parentPermissionString );

    boolean impliedBy( @Nonnull Permission parentPermission );

    @Nonnull
    List<List<String>> getTokens();
}
