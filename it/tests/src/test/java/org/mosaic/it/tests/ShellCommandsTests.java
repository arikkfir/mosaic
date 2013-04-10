package org.mosaic.it.tests;

import javax.annotation.Nonnull;
import org.junit.Test;
import org.mosaic.it.runner.ServerBootstrap;

import static java.util.regex.Pattern.quote;

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
                assertTrue( "Help not printed fully or not at all", output.matches( "\\b" + quote( "org.mosaic.api" ) + "\\b" ) );
                assertTrue( "Help not printed fully or not at all", output.matches( "\\b" + quote( "org.mosaic.lifecycle" ) + "\\b" ) );
                assertTrue( "Help not printed fully or not at all", output.matches( "\\b" + quote( "org.mosaic.core" ) + "\\b" ) );

                serverBootstrap.deployTestModule( "01" );
                assertTrue( "Help not printed fully or not at all", output.matches( "\\b" + quote( "org.mosaic.api" ) + "\\b" ) );
                assertTrue( "Help not printed fully or not at all", output.matches( "\\b" + quote( "org.mosaic.lifecycle" ) + "\\b" ) );
                assertTrue( "Help not printed fully or not at all", output.matches( "\\b" + quote( "org.mosaic.core" ) + "\\b" ) );
                assertTrue( "Help not printed fully or not at all", output.matches( "\\b" + quote( "test-module-01" ) + "\\b" ) );
            }
        } );
    }
}
