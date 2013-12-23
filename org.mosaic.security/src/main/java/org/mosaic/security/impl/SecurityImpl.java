package org.mosaic.security.impl;

import javax.annotation.Nonnull;
import org.mosaic.modules.Component;
import org.mosaic.modules.Service;
import org.mosaic.security.*;

/**
 * @author arik
 */
@Service
final class SecurityImpl implements Security
{
    private final Subject anonymous = new SubjectImpl( "anonymous" );

    @Nonnull
    private final ThreadLocal<Subject> subjectHolder = new InheritableThreadLocal<Subject>()
    {
        @Override
        protected Subject initialValue()
        {
            return anonymous;
        }
    };

    @Nonnull
    @Component
    private RealmManager realmManager;

    @Nonnull
    @Component
    private PermissionPolicyManager permissionPolicyManager;

    @Nonnull
    @Override
    public Subject authenticate( @Nonnull String realmName,
                                 @Nonnull String permissionPolicyName,
                                 @Nonnull AuthenticationToken authenticationToken ) throws AuthenticationException
    {
        AuthenticationResult authenticationResult = this.realmManager.authenticate( realmName, authenticationToken );
        return new SubjectImpl( authenticationResult, permissionPolicyName );
    }

    @Nonnull
    @Override
    public final Subject getSubject()
    {
        return this.subjectHolder.get();
    }

    @Nonnull
    @Override
    public Subject getAnonymousSubject()
    {
        return this.anonymous;
    }

    private class SubjectImpl extends AbstractSubject
    {
        @Nonnull
        private final String permissionPolicyName;

        private SubjectImpl( @Nonnull String subjectName )
        {
            super( subjectName );
            this.permissionPolicyName = "localUsers";
        }

        private SubjectImpl( @Nonnull AuthenticationResult result, @Nonnull String permissionPolicyName )
        {
            super( result );
            this.permissionPolicyName = permissionPolicyName;
        }

        @Override
        public boolean hasPermission( @Nonnull String permission )
        {
            Permission p = Permission.get( permission );
            return SecurityImpl.this.permissionPolicyManager.isPermitted( this.permissionPolicyName, this, p );
        }

        @Override
        public boolean hasPermission( @Nonnull Permission permission )
        {
            return SecurityImpl.this.permissionPolicyManager.isPermitted( this.permissionPolicyName, this, permission );
        }

        @Override
        public void login()
        {
            SecurityImpl.this.subjectHolder.set( this );
        }

        @Override
        public void logout()
        {
            SecurityImpl.this.subjectHolder.set( SecurityImpl.this.anonymous );
        }
    }
}
