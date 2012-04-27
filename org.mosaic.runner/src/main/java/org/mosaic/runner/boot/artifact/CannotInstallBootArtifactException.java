package org.mosaic.runner.boot.artifact;


import org.mosaic.runner.ExitCode;
import org.mosaic.runner.SystemExitException;

/**
 * @author arik
 */
public class CannotInstallBootArtifactException extends SystemExitException {

    private final BootArtifact artifact;

    public CannotInstallBootArtifactException( BootArtifact artifact, String message ) {
        super( "Artifact '" + artifact + "' cannot be resolved: " + message, ExitCode.CONFIG_ERROR );
        this.artifact = artifact;
    }

    public CannotInstallBootArtifactException( BootArtifact artifact, Throwable cause ) {
        super( "Artifact '" + artifact + "' cannot be resolved: " + cause.getMessage(), cause, ExitCode.CONFIG_ERROR );
        this.artifact = artifact;
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public BootArtifact getArtifact() {
        return artifact;
    }
}
