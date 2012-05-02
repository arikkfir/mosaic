package org.mosaic.runner.boot.artifact.resolve;

import java.util.Set;
import org.mosaic.runner.ServerHome;
import org.mosaic.runner.boot.artifact.BootArtifact;
import org.mosaic.runner.boot.artifact.CannotInstallBootArtifactException;
import org.osgi.framework.Bundle;

/**
 * @author arik
 */
public interface BootArtifactResolver
{
    Set<Bundle> resolve( ServerHome home, BootArtifact artifact ) throws CannotInstallBootArtifactException;

}
