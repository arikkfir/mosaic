package org.mosaic.security.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.lifecycle.annotation.BeanRef;
import org.mosaic.lifecycle.annotation.Service;
import org.mosaic.lifecycle.annotation.ServiceRef;
import org.mosaic.security.User;
import org.mosaic.security.UserManager;
import org.mosaic.security.realm.Realm;
import org.mosaic.security.realm.RealmManager;
import org.mosaic.util.convert.ConversionService;

/**
 * @author arik
 */
@Service( UserManager.class )
public class UserManagerImpl implements UserManager
{
    @Nonnull
    private static final AnonymousUserImpl ANONYMOUS_USER = new AnonymousUserImpl();

    @Nonnull
    private final ThreadLocal<UserScope> userScopeHolder = new ThreadLocal<>();

    @Nonnull
    private RealmManager realmManager;

    @Nonnull
    private ConversionService conversionService;

    @BeanRef
    public void setRealmManager( @Nonnull RealmManager realmManager )
    {
        this.realmManager = realmManager;
    }

    @ServiceRef
    public void setConversionService( @Nonnull ConversionService conversionService )
    {
        this.conversionService = conversionService;
    }

    @Nullable
    @Override
    public User loadUser( @Nonnull String realmName, @Nonnull String userName )
    {
        Realm realm = this.realmManager.getRealm( realmName );
        if( realm == null )
        {
            return null;
        }
        else
        {
            UserImpl user = new UserImpl( this.conversionService, userName );
            boolean loaded = realm.loadUser( user );
            return loaded ? user : null;
        }
    }

    @Nonnull
    @Override
    public User getAnonymousUser()
    {
        return ANONYMOUS_USER;
    }

    @Nonnull
    @Override
    public User getUser()
    {
        UserScope userScope = this.userScopeHolder.get();
        if( userScope == null )
        {
            return ANONYMOUS_USER;
        }

        User user = userScope.getUser();
        return user == null ? ANONYMOUS_USER : user;
    }

    @Override
    public void setUser( @Nonnull User user )
    {
        UserScope userScope = this.userScopeHolder.get();
        if( userScope == null )
        {
            throw new IllegalStateException( "No user scope has been set for thread '" + Thread.currentThread().getName() + "'" );
        }
        else
        {
            userScope.setUser( user );
        }
    }

    @Override
    public void setUserScope( @Nullable UserScope userScope )
    {
        if( userScope != null )
        {
            this.userScopeHolder.set( userScope );
        }
        else
        {
            this.userScopeHolder.remove();
        }
    }
}
