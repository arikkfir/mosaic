package org.mosaic.server.transaction;

import javax.sql.DataSource;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author arik
 * @todo we should avoid exposing PlatformTransactionManager here and provide our own API (its implementation should redirect to the Spring API)
 */
public interface TransactionManager extends DataSource, PlatformTransactionManager {

}
