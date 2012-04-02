package org.mosaic.runner;

/**
 * Thrown when a configuration error occurs that prevents Mosaic from starting.
 *
 * @author arik
 */
public class ConfigurationException extends SystemExitException {

    /**
     * Creates an instance with the specified causing exception.
     *
     * @param message the configuration error description
     */
    public ConfigurationException( String message, Throwable cause ) {
        super( message, cause, ExitCode.CONFIG_ERROR );
    }
}
