package org.mosaic.server.transaction;

import javax.sql.DataSource;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author arik
 */
public interface TransactionManager extends DataSource, PlatformTransactionManager {

}
