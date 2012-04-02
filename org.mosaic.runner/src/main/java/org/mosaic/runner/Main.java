package org.mosaic.runner;

import static com.google.inject.Guice.createInjector;

/**
 * @author arik
 */
public class Main {

    public static void main( String[] args ) {
        try {
            //
            // bootstrap
            //
            Runner runner = createInjector().getInstance( Runner.class );

            //
            // run and return exit code to OS
            //
            int exitCode = runner.run().getCode();
            System.exit( exitCode );

        } catch( SystemExitException e ) {
            e.printStackTrace( System.err );
            System.exit( e.getExitCode().getCode() );

        } catch( Exception e ) {
            e.printStackTrace( System.err );
            System.exit( ExitCode.UNKNOWN_ERROR.getCode() );
        }
    }
}
