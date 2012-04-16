package org.mosaic.server.db.impl;

import java.io.PrintWriter;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.mosaic.logging.LoggerFactory;

/**
 * @author arik
 */
public abstract class BaseDataSource implements DataSource {

    protected final String name;

    protected final org.mosaic.logging.Logger logger;

    protected final PrintWriter printWriter;

    protected BaseDataSource( String name ) {
        this.name = name;

        String baseLoggerName = LoggerFactory.getBundleLogger( getClass() ).getName();
        this.logger = LoggerFactory.getLogger( baseLoggerName + "." + this.name );
        this.printWriter = this.logger.getPrintWriter();
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return this.printWriter;
    }

    @Override
    public void setLogWriter( PrintWriter out ) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setLoginTimeout( int seconds ) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        // the spec defines that zero is a valid value, indicating that there is no time-out
        return 0;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public <T> T unwrap( Class<T> type ) throws SQLException {
        throw new SQLException( "not a wrapping DataSource implementation" );
    }

    @Override
    public boolean isWrapperFor( Class<?> type ) throws SQLException {
        return false;
    }
}
