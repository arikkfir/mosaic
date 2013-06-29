package org.mosaic.security.policy.impl;

import javax.annotation.Nonnull;
import org.mosaic.security.User;
import org.mosaic.security.policy.Permission;
import org.mosaic.util.expression.Expression;

/**
 * @author arik
 */
public class GrantRule
{
    @Nonnull
    private final Expression test;

    @Nonnull
    private final Permission permission;

    public GrantRule( @Nonnull Expression test, @Nonnull Permission permission )
    {
        this.test = test;
        this.permission = permission;
    }

    public boolean implies( @Nonnull User user, @Nonnull String permission )
    {
        if( this.permission.implies( permission ) )
        {
            return this.test.createInvoker()
                            .withRoot( user )
                            .expect( Boolean.class )
                            .require();
        }
        else
        {
            return false;
        }
    }
}
