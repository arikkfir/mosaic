package org.mosaic.it.tests;

import javax.annotation.Nonnull;
import org.junit.Test;
import org.mosaic.it.runner.CallableWithMosaic;
import org.mosaic.it.runner.CommandResult;
import org.mosaic.it.runner.MosaicRunner;

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
                runner.deployWebApplication( "app01" );

                // check again
                runner.runCommand( "checkWebAppWired" ).assertSuccess();
                return null;
            }
        } );
    }
}
