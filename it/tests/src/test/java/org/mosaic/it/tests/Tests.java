package org.mosaic.it.tests;

import java.io.IOException;
import java.sql.SQLException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mosaic.it.runner.ServerBootstrap;

/**
 * @author arik
 */
public class Tests extends Assert
{
    private ServerBootstrap bootstrap;

    @Before
    public void setup() throws IOException, InterruptedException, SQLException
    {
        this.bootstrap = new ServerBootstrap();
        this.bootstrap.start();
    }

    @After
    public void tearDown() throws IOException, InterruptedException
    {
        if( this.bootstrap != null )
        {
            this.bootstrap.shutdown();
        }
        this.bootstrap = null;
    }

    @Test
    public void testServiceRefOnServer() throws IOException, InterruptedException
    {
        this.bootstrap.deployTestModule( "01" );
        Thread.sleep( 4000 );
        assertTrue( "@ServiceRef did not execute", this.bootstrap.checkFileExists( "work/serviceRefOnServer.result" ) );
    }
}
