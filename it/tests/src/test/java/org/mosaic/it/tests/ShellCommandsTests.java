package org.mosaic.it.tests;

import javax.annotation.Nonnull;
import org.junit.Test;
import org.mosaic.it.runner.ServerBootstrap;

/**
 * @author arik
 */
public class ShellCommandsTests extends BaseServerTest
{
    @Test
    public void testHelp() throws Exception
    {
        doWithServer( new ServerRunnable()
        {
            @Override
            public void run( @Nonnull ServerBootstrap serverBootstrap ) throws Exception
            {
                String output = serverBootstrap.executeCommand( "org.mosaic.core:help" ).getOutput();
                assertTrue( "Help not printed fully or not at all", output.contains( "org.mosaic.core:metrics" ) );
                assertTrue( "Help not printed fully or not at all", output.contains( "org.mosaic.core:help" ) );
                assertTrue( "Help not printed fully or not at all", output.contains( "org.mosaic.core:restart-server" ) );
                assertTrue( "Help not printed fully or not at all", output.contains( "org.mosaic.core:list-modules" ) );
                assertTrue( "Help not printed fully or not at all", output.contains( "org.mosaic.core:inspect-module" ) );
                assertTrue( "Help not printed fully or not at all", output.contains( "org.mosaic.core:shutdown-server" ) );
            }
        } );
    }

    @Test
    public void testDeployModule() throws Exception
    {
        doWithServer( new ServerRunnable()
        {
            @Override
            public void run( @Nonnull ServerBootstrap serverBootstrap ) throws Exception
            {
                serverBootstrap.deployTestModule( "01" );
            }
        } );
        doWithServer( new ServerRunnable()
        {
            @Override
            public void run( @Nonnull ServerBootstrap serverBootstrap ) throws Exception
            {
                serverBootstrap.deployTestModule( "01" );
                ServerBootstrap.CommandResult result = serverBootstrap.executeCommand( "test-module-01:checkServerInjected" );
                assertEquals( "Server service not injected properly", 0, result.getExitCode() );
            }
        } );
    }

    @Test
    public void testListModules() throws Exception
    {
        doWithServer( new ServerRunnable()
        {
            @Override
            public void run( @Nonnull ServerBootstrap serverBootstrap ) throws Exception
            {
                String output = serverBootstrap.executeCommand( "org.mosaic.core:list-modules" ).getOutput();
                System.out.println( "List modules output:" );
                System.out.println( output );

                assertTrue( "Mosaic API bundle not found", output.contains( "org.mosaic.api" ) );
                assertTrue( "Mosaic lifecycle bundle not found", output.contains( "org.mosaic.lifecycle" ) );
                assertTrue( "Mosaic core bundle not found", output.contains( "org.mosaic.core" ) );

                serverBootstrap.deployTestModule( "01" );
                output = serverBootstrap.executeCommand( "org.mosaic.core:list-modules" ).getOutput();
                System.out.println( "List modules output (after deployment):" );
                System.out.println( output );
                assertTrue( "Mosaic API bundle not found", output.contains( "org.mosaic.api" ) );
                assertTrue( "Mosaic lifecycle bundle not found", output.contains( "org.mosaic.lifecycle" ) );
                assertTrue( "Mosaic core bundle not found", output.contains( "org.mosaic.core" ) );
                assertTrue( "Test module 01 bundle not found", output.contains( "test-module-01" ) );
            }
        } );
    }
}
