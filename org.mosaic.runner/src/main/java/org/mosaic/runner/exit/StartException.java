package org.mosaic.runner.exit;

/**
 * @author arik
 */
public class StartException extends SystemExitException {

    public StartException( String message ) {
        super( message, ExitCode.START_ERROR );
    }

    public StartException( String message, Throwable cause ) {
        super( message, cause, ExitCode.START_ERROR );
    }
}
