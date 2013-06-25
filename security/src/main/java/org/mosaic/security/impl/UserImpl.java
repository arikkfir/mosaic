package org.mosaic.security.impl;

import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.security.CredentialNotFoundException;
import org.mosaic.security.Principal;
import org.mosaic.security.PrincipalNotFoundException;
import org.mosaic.security.realm.MutableUser;
import org.mosaic.util.collect.ConcurrentHashMapEx;
import org.mosaic.util.collect.HashMapEx;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.convert.ConversionService;

/**
 * @author arik
 */
public class UserImpl extends ConcurrentHashMapEx<String, Object> implements MutableUser
{
    @Nonnull
    private final String name;

    @Nullable
    private Set<String> roles;

    @Nullable
    private MapEx<String, Principal> principalsByName;

    @Nullable
    private MapEx<Class<? extends Principal>, Principal> principalsByType;

    @Nullable
    private MapEx<Class<?>, Object> credentialsByType;

    public UserImpl( @Nonnull ConversionService conversionService, @Nonnull String name )
    {
        super( 20, conversionService );
        this.name = name;
    }

    @Override
    public boolean isAnonymous()
    {
        return false;
    }

    @Nonnull
    @Override
    public String getName()
    {
        return this.name;
    }

    @Nonnull
    @Override
    public Set<String> getRoles()
    {
        if( this.roles == null )
        {
            return Collections.emptySet();
        }
        else
        {
            return this.roles;
        }
    }

    @Nonnull
    @Override
    public MutableUser addRole( @Nonnull String role )
    {
        if( this.roles == null )
        {
            this.roles = new LinkedHashSet<>( 10 );
        }
        this.roles.add( role );
        return this;
    }

    @Override
    public boolean is( @Nonnull String type )
    {
        return this.principalsByName != null && this.principalsByName.containsKey( type );
    }

    @Nullable
    @Override
    public Principal getPrincipal( @Nonnull String type )
    {
        return this.principalsByName == null ? null : this.principalsByName.get( type );
    }

    @Nullable
    @Override
    public <T extends Principal> T getPrincipal( @Nonnull Class<T> type )
    {
        return this.principalsByType == null ? null : this.principalsByType.get( type, type );
    }

    @Nonnull
    @Override
    public <T extends Principal> T requirePrincipal( @Nonnull Class<T> type )
    {
        if( this.principalsByType == null )
        {
            throw new PrincipalNotFoundException( "Principal of type '" + type.getName() + "' not found for user '" + this.name + "'" );
        }
        else
        {
            return this.principalsByType.require( type, type );
        }
    }

    @Nonnull
    @Override
    public Map<String, Principal> getPrincipals()
    {
        if( this.principalsByName == null )
        {
            return Collections.emptyMap();
        }
        else
        {
            return this.principalsByName;
        }
    }

    @Nonnull
    @Override
    public MutableUser addPrincipal( @Nonnull Principal principal )
    {
        if( this.principalsByName == null )
        {
            this.principalsByName = new HashMapEx<>( conversionService );
        }
        this.principalsByName.put( principal.getType(), principal );

        if( this.principalsByType == null )
        {
            this.principalsByType = new HashMapEx<>( conversionService );
        }
        this.principalsByType.put( principal.getClass(), principal );

        return this;
    }

    @Nullable
    @Override
    public <T> T getCredential( @Nonnull Class<T> type )
    {
        return this.credentialsByType == null ? null : this.credentialsByType.get( type, type );
    }

    @Nonnull
    @Override
    public MutableUser addCredential( @Nonnull Object credential )
    {
        if( this.credentialsByType == null )
        {
            this.credentialsByType = new HashMapEx<>( conversionService );
        }
        this.credentialsByType.put( credential.getClass(), credential );
        return this;
    }

    @Nonnull
    @Override
    public <T> T requireCredential( @Nonnull Class<T> type )
    {
        if( this.credentialsByType == null )
        {
            throw new CredentialNotFoundException( "Credential of type '" + type.getName() + "' not found for user '" + this.name + "'" );
        }
        else
        {
            return this.credentialsByType.require( type, type );
        }
    }

    @Nonnull
    @Override
    public Collection<Object> getCredentials()
    {
        if( this.credentialsByType == null )
        {
            return Collections.emptyList();
        }
        else
        {
            return this.credentialsByType.values();
        }
    }
}
