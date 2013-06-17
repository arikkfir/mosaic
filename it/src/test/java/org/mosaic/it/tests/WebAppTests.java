package org.mosaic.it.tests;

import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.Nonnull;
import org.junit.Test;
import org.mosaic.it.runner.CallableWithMosaic;
import org.mosaic.it.runner.CommandResult;
import org.mosaic.it.runner.MosaicRunner;

import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 * @author arik
 */
public class WebAppTests extends BaseTests
{
    @Test
    public void testWebAppParse() throws Exception
    {
        this.runner.runOnServer( new CallableWithMosaic<Object>()
        {
            @Override
            public Object run( @Nonnull MosaicRunner runner ) throws Exception
            {
                runner.deployModule( "module02" );

                // check that no web-app was injected...
                CommandResult resultBeforeAppDescDeployed = runner.runCommand( "checkWebAppWired" );
                if( resultBeforeAppDescDeployed.getExitCode() != -1 )
                {
                    fail( resultBeforeAppDescDeployed.getOutput() );
                }

                // deploy the web-app descriptor
                Path appFile = runner.deployWebApplication( "app01" );

                // check again
                runner.runCommand( "checkWebAppWired" ).assertSuccess();

                // modify app and test its updated
                String contents = new String( Files.readAllBytes( appFile ), "UTF-8" );
                contents = contents.replace( "/some/dir/module02", "/some/dir/module02-updated" );
                Files.write( appFile, contents.getBytes( "UTF-8" ), WRITE, TRUNCATE_EXISTING );
                Thread.sleep( 10000 );
                runner.runCommand( "checkWebAppContentRoot" ).assertSuccess();

                return null;
            }
        } );
    }
}
