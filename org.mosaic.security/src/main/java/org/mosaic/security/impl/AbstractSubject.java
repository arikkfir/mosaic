package org.mosaic.security.impl;

import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.security.AuthenticationResult;
import org.mosaic.security.Permission;
import org.mosaic.security.Principal;
import org.mosaic.security.Subject;

import static com.google.common.collect.Iterators.unmodifiableIterator;

/**
 * @author arik
 */
abstract class AbstractSubject implements Subject
{
    @Nonnull
    private final String subjectName;

    private final boolean authenticated;

    @Nonnull
    private final Set<String> roles;

    @Nonnull
    private final TypedCollectionImpl<Principal> principals;

    @Nonnull
    private final TypedCollectionImpl<Object> credentials;

    protected AbstractSubject( @Nonnull String subjectName )
    {
        this.subjectName = subjectName;
        this.authenticated = false;
        this.roles = Collections.emptySet();
        this.principals = new TypedCollectionImpl<>( Collections.<Principal>emptyList() );
        this.credentials = new TypedCollectionImpl<>( Collections.<Object>emptyList() );
    }

    protected AbstractSubject( @Nonnull AuthenticationResult result )
    {
        this.subjectName = result.getSubjectName();
        this.authenticated = true;
        this.roles = Collections.unmodifiableSet( new HashSet<>( result.getRoles() ) );
        this.principals = new TypedCollectionImpl<>( result.getPrincipals() );
        this.credentials = new TypedCollectionImpl<>( result.getCredentials() );
    }

    @Nonnull
    @Override
    public String getName()
    {
        return this.subjectName;
    }

    @Override
    public boolean isAuthenticated()
    {
        return this.authenticated;
    }

    @Nonnull
    @Override
    public Set<String> getRoles()
    {
        return this.roles;
    }

    @Override
    public boolean is( @Nonnull Class<? extends Principal> principalType )
    {
        for( Principal principal : this.principals )
        {
            if( principalType.isInstance( principal ) )
            {
                return true;
            }
        }
        return false;
    }

    @Nonnull
    @Override
    public Subject.TypedCollection<Principal> getPrincipals()
    {
        return this.principals;
    }

    @Nonnull
    @Override
    public Subject.TypedCollection<Object> getCredentials()
    {
        return this.credentials;
    }

    @Override
    public boolean hasPermission( @Nonnull String permission )
    {
        return false;
    }

    @Override
    public boolean hasPermission( @Nonnull Permission permission )
    {
        return false;
    }

    private class TypedCollectionImpl<Type> implements Subject.TypedCollection<Type>
    {
        @Nonnull
        private final Set<Type> values;

        private TypedCollectionImpl( Collection<? extends Type> c )
        {
            this.values = new LinkedHashSet<>( c );
        }

        @Override
        public int size()
        {
            return this.values.size();
        }

        @Override
        public boolean isEmpty()
        {
            return this.values.isEmpty();
        }

        @Override
        public boolean contains( Object o )
        {
            return this.values.contains( o );
        }

        @Nonnull
        @Override
        public Iterator<Type> iterator()
        {
            return unmodifiableIterator( this.values.iterator() );
        }

        @Nonnull
        @Override
        public Object[] toArray()
        {
            return this.values.toArray();
        }

        @Nonnull
        @Override
        public <T> T[] toArray( @Nonnull T[] a )
        {
            //noinspection SuspiciousToArrayCall
            return this.values.toArray( a );
        }

        @Override
        public boolean add( Type type )
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove( Object o )
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsAll( @Nonnull Collection<?> c )
        {
            return this.values.containsAll( c );
        }

        @Override
        public boolean addAll( @Nonnull Collection<? extends Type> c )
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll( @Nonnull Collection<?> c )
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll( @Nonnull Collection<?> c )
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear()
        {
            throw new UnsupportedOperationException();
        }

        @Nullable
        @Override
        public <T extends Type> T firstOfType( @Nonnull Class<T> type )
        {
            for( Type value : this.values )
            {
                if( type.isInstance( value ) )
                {
                    return type.cast( value );
                }
            }
            return null;
        }

        @Nonnull
        @Override
        public <T extends Type> Collection<? extends T> ofType( @Nonnull Class<T> type )
        {
            Collection<T> matches = null;
            for( Type value : this.values )
            {
                if( type.isInstance( value ) )
                {
                    if( matches == null )
                    {
                        matches = new LinkedList<>();
                    }
                    matches.add( type.cast( value ) );
                }
            }
            return matches == null ? Collections.<T>emptyList() : matches;
        }
    }
}
