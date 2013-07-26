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
import org.mosaic.util.collect.EmptyMapEx;
import org.mosaic.util.collect.MapEx;

/**
 * @author arik
 */
public class AnonymousUserImpl implements MutableUser
{
    @Override
    public String toString()
    {
        return "User[anonymous]";
    }

    @Nonnull
    @Override
    public MapEx<String, Object> getAttributes()
    {
        return EmptyMapEx.emptyMapEx();
    }

    @Override
    public boolean isAnonymous()
    {
        return true;
    }

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
}
