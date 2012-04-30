package org.mosaic.security;

import org.mosaic.util.collection.TypedDict;

/**
 * @author arik
 */
public interface User extends TypedDict<Object> {

    interface Credential {

        String getName();

        String getType();

    }

    String getName();

    String getDisplayName();

    <T extends Credential> T getCredential( Class<T> type );

    <T extends Credential> T requireCredential( Class<T> type );

    boolean hasPermission( String permission );

}
