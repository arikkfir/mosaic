package org.mosaic.it.it01.tests;

import java.io.IOException;
import java.nio.file.Paths;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mosaic.it.runner.ServerBootstrap;

/**
 * @author arik
 */
public class ServerStartupTests
{
    private ServerBootstrap bootstrap;

    @Before
    public void setup() throws IOException, InterruptedException
    {
        this.bootstrap = new ServerBootstrap();
        this.bootstrap.start();
    }

    @After
    public void tearDown() throws IOException, InterruptedException
    {
        this.bootstrap.shutdown();
        this.bootstrap = null;
    }

    @Test
    public void test01() throws IOException, InterruptedException
    {
        this.bootstrap.deploy( Paths.get( System.getProperty( "user.dir" ), "..", "moduleA", "target", "moduleA.jar" ) );
        Thread.sleep( 5000 );
    }
}
