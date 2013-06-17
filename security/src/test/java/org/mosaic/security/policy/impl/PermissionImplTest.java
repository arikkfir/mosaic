package org.mosaic.security.policy.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.junit.Assert;
import org.junit.Test;
import org.mosaic.security.policy.Permission;
import org.mosaic.security.policy.PermissionParseException;
import org.mosaic.security.policy.PermissionPoliciesManager;
import org.mosaic.security.policy.PermissionPolicy;

/**
 * @author arik
 */
public class PermissionImplTest extends Assert
{
    private MockPermissionPoliciesManager permissionPoliciesManager = new MockPermissionPoliciesManager();

    @Test
    public void testPermission()
    {
        assertTrue( permission( "a:b:c" ).implies( "a:b:c" ) );
        assertTrue( permission( "a:b:*" ).implies( "a:b:c" ) );
        assertTrue( permission( "a:b" ).implies( "a:b:c" ) );
        assertTrue( permission( "a:*" ).implies( "a:b:c" ) );
        assertTrue( permission( "*:b" ).implies( "a:b:c" ) );

        assertFalse( permission( "a:b:c" ).implies( "a:b:z" ) );
        assertFalse( permission( "a:b:c" ).implies( "a:a:c" ) );
        assertFalse( permission( "a:b:*" ).implies( "a:a:c" ) );
    }

    @Test(expected = PermissionParseException.class)
    public void testBadPermission1()
    {
        permission( "a:b:" );
    }

    @Test(expected = PermissionParseException.class)
    public void testBadPermission2()
    {
        permission( "a::c" );
    }

    private Permission permission( String permission )
    {
        return this.permissionPoliciesManager.parsePermission( permission );
    }

    private class MockPermissionPoliciesManager implements PermissionPoliciesManager
    {
        @Nonnull
        @Override
        public Permission parsePermission( @Nonnull String permission )
        {
            return new PermissionImpl( this, permission );
        }

        @Nullable
        @Override
        public PermissionPolicy getPolicy( @Nonnull String name )
        {
            throw new UnsupportedOperationException();
        }
    }
}
