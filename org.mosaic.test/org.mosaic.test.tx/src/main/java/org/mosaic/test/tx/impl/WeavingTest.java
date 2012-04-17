package org.mosaic.test.tx.impl;

import org.mosaic.server.shell.ShellCommand;
import org.mosaic.transaction.Transactional;
import org.springframework.stereotype.Component;

/**
 * @author arik
 */
@Component
public class WeavingTest {

    @Transactional
    @ShellCommand( "test-tx" )
    public void testWithTx() {
        System.out.println( "testWithTx invoked" );
    }

    @ShellCommand( "test-no-tx" )
    public void testWithoutTx() {
        System.out.println( "testWithoutTx invoked" );
    }
}
