package org.mosaic.security.realm.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.lifecycle.MethodEndpoint;
import org.mosaic.lifecycle.annotation.Service;
import org.mosaic.lifecycle.annotation.ServiceBind;
import org.mosaic.lifecycle.annotation.ServiceUnbind;
import org.mosaic.security.realm.MutableUser;
import org.mosaic.security.realm.Realm;
import org.mosaic.security.realm.RealmManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
@Service( RealmManager.class )
public class RealmManagerImpl implements RealmManager
{
    private static final UserParameterResolver USER_PARAMETER_RESOLVER = new UserParameterResolver();

    private static final Logger LOG = LoggerFactory.getLogger( RealmManagerImpl.class );

    @Nonnull
    private final Map<String, Realm> realms = new ConcurrentHashMap<>( 10 );

    @ServiceBind
    public void addRealm( @Nonnull Realm realm )
    {
        this.realms.put( realm.getName(), realm );
    }

    @ServiceUnbind
    public void removeRealm( @Nonnull Realm realm )
    {
        this.realms.remove( realm.getName() );
    }

    @ServiceBind( "type=org.mosaic.security.realm.annotation.Realm" )
    public void addRealmMethodEndpoint( @Nonnull MethodEndpoint realmMethodEndpoint )
    {
        RealmMethodEndpointAdapter realmAdapter = new RealmMethodEndpointAdapter( realmMethodEndpoint );
        this.realms.put( realmAdapter.getName(), realmAdapter );
    }

    @ServiceUnbind( "type=org.mosaic.security.realm.annotation.Realm" )
    public void removeRealmMethodEndpoint( @Nonnull MethodEndpoint realmMethodEndpoint )
    {
        org.mosaic.security.realm.annotation.Realm ann = realmMethodEndpoint.getAnnotation( org.mosaic.security.realm.annotation.Realm.class );
        if( ann != null )
        {
            this.realms.remove( ann.value() );
        }
    }

    @Nullable
    @Override
    public Realm getRealm( @Nonnull String name )
    {
        return this.realms.get( name );
    }

    private class RealmMethodEndpointAdapter implements Realm
    {
        @Nonnull
        private final MethodEndpoint endpoint;

        @Nonnull
        private final String name;

        private RealmMethodEndpointAdapter( @Nonnull MethodEndpoint endpoint )
        {
            this.endpoint = endpoint;
            org.mosaic.security.realm.annotation.Realm ann = this.endpoint.getAnnotation( org.mosaic.security.realm.annotation.Realm.class );
            if( ann == null )
            {
                throw new IllegalStateException( "MethodEndpoint '" + endpoint + "' is not a @Realm endpoint" );
            }
            this.name = ann.value();
        }

        @Nonnull
        @Override
        public String getName()
        {
            return this.name;
        }

        @Override
        public boolean loadUser( @Nonnull MutableUser user )
        {
            Map<String, Object> context = new HashMap<>( 1 );
            context.put( "user", user );
            try
            {
                Object result = this.endpoint.createInvoker( USER_PARAMETER_RESOLVER ).resolve( context ).invoke();
                return result instanceof Boolean && ( Boolean ) result;
            }
            catch( Exception e )
            {
                LOG.warn( "Realm '{}' failed while loading user '{}': {}", this.name, user, e.getMessage(), e );
                return false;
            }
        }
    }
}
