package org.mosaic.server.security;

import org.mosaic.security.User;

/**
 * @author arik
 */
public abstract class Security
{
    private static final ThreadLocal<User> USER = new ThreadLocal<>();

    public static User user()
    {
        return USER.get();
    }

    public static void setUser( User user )
    {
        USER.set( user );
    }

}
