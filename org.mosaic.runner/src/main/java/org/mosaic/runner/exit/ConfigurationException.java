package org.mosaic.runner.exit;

/**
 * Thrown when a configuration error occurs that prevents Mosaic from starting.
 *
 * @author arik
 */
public class ConfigurationException extends SystemExitException {

    /**
     * Creates an instance with no causing exception.
     *
     * @param message the configuration error description
     */
    public ConfigurationException( String message ) {
        super( message, ExitCode.CONFIG_ERROR );
    }

    /**
     * Creates an instance with the specified causing exception.
     *
     * @param message the configuration error description
     */
    public ConfigurationException( String message, Throwable cause ) {
        super( message, cause, ExitCode.CONFIG_ERROR );
    }
}
