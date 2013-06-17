package org.mosaic.security.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.security.CredentialNotFoundException;
import org.mosaic.security.Principal;
import org.mosaic.security.PrincipalNotFoundException;
import org.mosaic.security.realm.MutableUser;
import org.mosaic.util.collect.RequiredKeyMissingException;

import static java.lang.String.format;

/**
 * @author arik
 */
public class AnonymousUserImpl implements MutableUser
{
    @Nonnull
    @Override
    public String getName()
    {
        return "anonymous";
    }

    @Nonnull
    @Override
    public Set<String> getRoles()
    {
        return Collections.emptySet();
    }

    @Nonnull
    @Override
    public MutableUser addRole( @Nonnull String role )
    {
        throw new UnsupportedOperationException( "Anonymous user is read-only" );
    }

    @Override
    public boolean is( @Nonnull String type )
    {
        return false;
    }

    @Nullable
    @Override
    public Principal getPrincipal( @Nonnull String type )
    {
        return null;
    }

    @Nullable
    @Override
    public <T extends Principal> T getPrincipal( @Nonnull Class<T> type )
    {
        return null;
    }

    @Nonnull
    @Override
    public <T extends Principal> T requirePrincipal( @Nonnull Class<T> type )
    {
        throw new PrincipalNotFoundException( "Principal of type '" + type.getName() + "' not found for user '" + getName() + "'" );
    }

    @Nonnull
    @Override
    public Map<String, Principal> getPrincipals()
    {
        return Collections.emptyMap();
    }

    @Nonnull
    @Override
    public MutableUser addPrincipal( @Nonnull Principal principal )
    {
        throw new UnsupportedOperationException( "Anonymous user is read-only" );
    }

    @Nullable
    @Override
    public <T> T getCredential( @Nonnull Class<T> type )
    {
        return null;
    }

    @Nonnull
    @Override
    public MutableUser addCredential( @Nonnull Object credential )
    {
        throw new UnsupportedOperationException( "Anonymous user is read-only" );
    }

    @Nonnull
    @Override
    public <T> T requireCredential( @Nonnull Class<T> type )
    {
        throw new CredentialNotFoundException( "Credential of type '" + type.getName() + "' not found for user '" + getName() + "'" );
    }

    @Nonnull
    @Override
    public Collection<Object> getCredentials()
    {
        return Collections.emptyList();
    }

    @Override
    public Object get( @Nonnull String key, @Nullable Object defaultValue )
    {
        return defaultValue;
    }

    @Nonnull
    @Override
    public Object require( @Nonnull String key )
    {
        throw new RequiredKeyMissingException( format( "no value for key '%s'", key ), key );
    }

    @Override
    public <T> T get( @Nonnull String key, @Nonnull Class<T> type )
    {
        return null;
    }

    @Nonnull
    @Override
    public <T> T require( @Nonnull String key, @Nonnull Class<T> type )
    {
        throw new RequiredKeyMissingException( format( "no value for key '%s'", key ), key );
    }

    @Override
    public <T> T get( @Nonnull String key, @Nonnull Class<T> type, @Nullable T defaultValue )
    {
        return defaultValue;
    }

    @Override
    public int size()
    {
        return 0;
    }

    @Override
    public boolean isEmpty()
    {
        return true;
    }

    @Override
    public boolean containsKey( Object key )
    {
        return false;
    }

    @Override
    public boolean containsValue( Object value )
    {
        return false;
    }

    @Override
    public Object get( Object key )
    {
        return null;
    }

    @Override
    public Object put( String key, Object value )
    {
        throw new UnsupportedOperationException( "Anonymous user is read-only" );
    }

    @Override
    public Object remove( Object key )
    {
        throw new UnsupportedOperationException( "Anonymous user is read-only" );
    }

    @Override
    public void putAll( @Nonnull Map<? extends String, ?> m )
    {
        throw new UnsupportedOperationException( "Anonymous user is read-only" );
    }

    @Override
    public void clear()
    {
        throw new UnsupportedOperationException( "Anonymous user is read-only" );
    }

    @Nonnull
    @Override
    public Set<String> keySet()
    {
        return Collections.emptySet();
    }

    @Nonnull
    @Override
    public Collection<Object> values()
    {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public Set<Entry<String, Object>> entrySet()
    {
        return Collections.emptySet();
    }
}
