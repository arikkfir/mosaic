package org.mosaic.transaction;

import java.sql.Connection;

/**
 * @author arik
 */
public interface ConnectionProxy extends Connection {

    Connection getTargetConnection();

}
