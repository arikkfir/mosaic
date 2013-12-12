package org.mosaic.security;

import java.util.*;
import javax.annotation.Nonnull;

/**
 * @author arik
 */
public class AuthenticationResult
{
    @Nonnull
    private final String subjectName;

    @Nonnull
    private final Set<String> roles;

    @Nonnull
    private final Set<Principal> principals;

    @Nonnull
    private final Collection<Object> credentials;

    public AuthenticationResult( @Nonnull String subjectName,
                                 @Nonnull Set<String> roles,
                                 @Nonnull Set<Principal> principals,
                                 @Nonnull Collection<Object> credentials )
    {
        this.subjectName = subjectName;
        this.roles = Collections.unmodifiableSet( new HashSet<>( roles ) );
        this.principals = Collections.unmodifiableSet( new HashSet<>( principals ) );
        this.credentials = Collections.unmodifiableList( new LinkedList<>( credentials ) );
    }

    @Nonnull
    public final String getSubjectName()
    {
        return this.subjectName;
    }

    @Nonnull
    public final Set<String> getRoles()
    {
        return this.roles;
    }

    @Nonnull
    public final Set<Principal> getPrincipals()
    {
        return this.principals;
    }

    @Nonnull
    public final Collection<Object> getCredentials()
    {
        return this.credentials;
    }
}
