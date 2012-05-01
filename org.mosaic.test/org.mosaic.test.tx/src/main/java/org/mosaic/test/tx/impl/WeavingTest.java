package org.mosaic.test.tx.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.intellij.lang.annotations.Language;
import org.mosaic.lifecycle.ServiceRef;
import org.mosaic.server.shell.ShellCommand;
import org.mosaic.transaction.Transactional;
import org.mosaic.util.logging.Trace;
import org.springframework.stereotype.Component;

/**
 * @author arik
 */
@Component
public class WeavingTest {

    @Language( "MySQL" )
    private static final String UPDATE_SQL = "UPDATE `customers`.`cloud_flare_customers` SET `status`='arik';";

    @Language( "MySQL" )
    private static final String SELECT_SQL = "SELECT `status` FROM `customers`.`cloud_flare_customers`;";

    private DataSource dataSource;

    @ServiceRef( filter = "name=main" )
    public void setDataSource( DataSource dataSource ) {
        this.dataSource = dataSource;
    }

    @Transactional
    @ShellCommand( "test-good" )
    @Trace
    public void testWithTx() throws SQLException {
        try( Connection connection = this.dataSource.getConnection() ) {
            try( PreparedStatement stmt = connection.prepareStatement( UPDATE_SQL ) ) {
                stmt.execute();
                System.out.println( "Updated database" );
            }
        }
        testParticipate();
    }

    @Transactional
    @Trace
    private void testParticipate() throws SQLException {
        try( Connection connection = this.dataSource.getConnection() ) {
            System.out.println( "Connection is: " + System.identityHashCode( connection ) );
            try( PreparedStatement stmt = connection.prepareStatement( SELECT_SQL ) ) {
                try( ResultSet rs = stmt.executeQuery() ) {
                    while( rs.next() ) {
                        String label = rs.getMetaData().getColumnLabel( 1 );
                        String value = rs.getString( 1 );
                        System.out.println( label + ": " + value );
                    }
                }
            }
        }
        throw new IllegalStateException( "ROLL THIS BACK!!!" );
    }
}
