package org.mosaic.runner;

/**
 * The set of possible OS exit codes for the Mosaic JVM process. These exit codes have meaning to the Mosaic shell
 * script and act as an instruction for it. For example, if the Mosaic JVM returns with the {@link #RESTART} exit code,
 * it signals the shell script to re-start the JVM process.
 *
 * @author arik
 */
public enum ExitCode {

    /**
     * Indicates that the server was successfully stopped under normal circumstances (user-initiated).
     */
    SUCCESS( 0 ),

    /**
     * Indicates that the server failed to start due to a configuration problem.
     */
    CONFIG_ERROR( 1 ),

    /**
     * Indicates that the server failed to start due to an error while actually starting the server.
     */
    START_ERROR( 2 ),

    /**
     * Indicates to the shell wrapper that the JVM needs to be re-started.
     */
    RESTART( 3 ),

    /**
     * Indicates that the server has stopped due to a runtime error.
     */
    RUNTIME_ERROR( 4 ),

    /**
     * Indicates that the server was terminated with a kill signal (either to the JVM process or to the main
     * loop thread).
     */
    INTERRUPTED( 5 ),

    /**
     * Indicates that the server terminated due to an unknown error.
     */
    UNKNOWN_ERROR( 6 );

    /**
     * The exit code to use for this enum instance.
     */
    private final int code;

    /**
     * Private constructor used above by the available enums.
     *
     * @param exitCode the exit code to use for terminating the server
     */
    private ExitCode( int exitCode ) {
        this.code = exitCode;
    }

    /**
     * Returns the exit code the JVM should exit with when this enum instance is used.
     *
     * @return the OS exit code
     */
    public int getCode() {
        return this.code;
    }
}
