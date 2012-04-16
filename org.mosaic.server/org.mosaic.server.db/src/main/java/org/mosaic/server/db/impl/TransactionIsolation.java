package org.mosaic.server.db.impl;

/**
 * @author arik
 */
@SuppressWarnings( "UnusedDeclaration" )
public enum TransactionIsolation {
    NONE,
    READ_COMMITTED,
    READ_UNCOMMITTED,
    REPEATABLE_READ,
    SERIALIZABLE
}
