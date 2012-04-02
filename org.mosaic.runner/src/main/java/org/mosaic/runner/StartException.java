package org.mosaic.runner;

/**
 * @author arik
 */
public class StartException extends SystemExitException {

    public StartException( String message, Throwable cause ) {
        super( message, cause, ExitCode.START_ERROR );
    }
}
