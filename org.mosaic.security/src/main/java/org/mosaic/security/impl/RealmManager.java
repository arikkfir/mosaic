package org.mosaic.security.impl;

import com.google.common.reflect.TypeToken;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.modules.*;
import org.mosaic.security.AuthenticationException;
import org.mosaic.security.AuthenticationResult;
import org.mosaic.security.AuthenticationToken;
import org.mosaic.security.Realm;
import org.mosaic.util.collections.HashMapEx;
import org.mosaic.util.collections.MapEx;
import org.mosaic.util.reflection.MethodParameter;
import org.mosaic.util.reflection.ParameterResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
@Component
final class RealmManager
{
    private static final Logger LOG = LoggerFactory.getLogger( RealmManager.class );

    private static final TypeToken<AuthenticationResult> AUTHENTICATION_RESULT_TYPE_TOKEN = TypeToken.of( AuthenticationResult.class );

    @Nonnull
    private final Map<Long, RealmHandler> realms = new ConcurrentHashMap<>();

    @Nonnull
    AuthenticationResult authenticate( @Nonnull String realmName, @Nonnull AuthenticationToken authenticationToken )
    {
        for( RealmHandler realm : this.realms.values() )
        {
            if( realmName.equals( realm.name ) )
            {
                return realm.authenticate( authenticationToken );
            }
        }
        throw new AuthenticationException( "unknown realm: " + realmName, authenticationToken );
    }

    @OnServiceAdded
    void addRealm( @Nonnull ServiceReference<MethodEndpoint<Realm>> reference )
    {
        MethodEndpoint<Realm> endpoint = reference.get();
        if( endpoint != null )
        {
            this.realms.put( reference.getId(), new RealmHandler( endpoint ) );
        }
    }

    @OnServiceRemoved
    void removeRealm( @Nonnull ServiceReference<MethodEndpoint<Realm>> reference )
    {
        this.realms.remove( reference.getId() );
    }

    private class RealmHandler
    {
        @Nonnull
        private final MethodEndpoint<Realm> endpoint;

        @Nonnull
        private final String name;

        private final MethodEndpoint.Invoker invoker;

        private RealmHandler( @Nonnull MethodEndpoint<Realm> endpoint )
        {
            this.endpoint = endpoint;
            this.name = this.endpoint.getType().value().isEmpty() ? this.endpoint.getName() : this.endpoint.getType().value();

            TypeToken<?> methodReturnType = this.endpoint.getMethodHandle().getReturnType();
            if( !AUTHENTICATION_RESULT_TYPE_TOKEN.isAssignableFrom( methodReturnType ) )
            {
                throw new IllegalArgumentException( "bad realm endpoint at '" + this.endpoint + "': @Realm method endpoints must return AuthenticationResult objects" );
            }

            this.invoker = this.endpoint.createInvoker(
                    new ParameterResolver()
                    {
                        @Nullable
                        @Override
                        public Object resolve( @Nonnull MethodParameter parameter,
                                               @Nonnull MapEx<String, Object> resolveContext )
                                throws Exception
                        {
                            TypeToken<?> parameterType = parameter.getType();
                            if( AuthenticationToken.class.isAssignableFrom( parameterType.getRawType() ) )
                            {
                                AuthenticationToken authToken = resolveContext.require( "token", AuthenticationToken.class );
                                if( parameterType.isAssignableFrom( authToken.getClass() ) )
                                {
                                    return authToken;
                                }
                                else
                                {
                                    String realmName = RealmHandler.this.name;
                                    String authTokenTypeName = authToken.getClass().getName();
                                    throw new IllegalArgumentException( "realm '" + realmName + "' does not authentication tokens of type '" + authTokenTypeName + "'" );
                                }
                            }
                            return SKIP;
                        }
                    }
            );
        }

        @Nonnull
        AuthenticationResult authenticate( @Nonnull AuthenticationToken token )
        {
            MapEx<String, Object> context = new HashMapEx<>();
            context.put( "token", token );

            Object result;
            try
            {
                result = this.invoker.resolve( context ).invoke();
            }
            catch( AuthenticationException e )
            {
                throw e;
            }
            catch( Throwable e )
            {
                LOG.error( "@Realm endpoint '{}' threw an exception while evaluating authentication token '{}': {}",
                           this.endpoint, token, e.getMessage(), e );
                throw new AuthenticationException( "internal authentication error: ", token );
            }

            if( result == null )
            {
                throw new AuthenticationException( "empty authentication result", token );
            }
            else
            {
                return ( AuthenticationResult ) result;
            }
        }
    }
}
