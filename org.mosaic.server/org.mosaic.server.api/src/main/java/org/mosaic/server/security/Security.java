package org.mosaic.server.security;

import org.mosaic.security.User;

/**
 * @author arik
 */
public abstract class Security {

    private static final ThreadLocal<User> USER = new ThreadLocal<>();

    public static User user() {
        return USER.get();
    }

    public static User requireUser() {
        User user = user();
        if( user == null ) {
            Thread thread = Thread.currentThread();
            throw new IllegalStateException( "User not bound to current thread (" + thread.getName() + "[" + thread.getId() + "])" );
        } else {
            return user;
        }
    }

    public static void setRequest( User user ) {
        USER.set( user );
    }

}
