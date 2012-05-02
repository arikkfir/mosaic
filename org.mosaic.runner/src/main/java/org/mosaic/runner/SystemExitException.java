package org.mosaic.runner;

/**
 * @author arik
 */
public class SystemExitException extends Exception
{

    private final ExitCode exitCode;

    public SystemExitException( String message, ExitCode exitCode )
    {
        super( message );
        this.exitCode = exitCode;
    }

    public SystemExitException( String message, Throwable cause, ExitCode exitCode )
    {
        super( message, cause );
        this.exitCode = exitCode;
    }

    public ExitCode getExitCode( )
    {
        return this.exitCode;
    }
}
