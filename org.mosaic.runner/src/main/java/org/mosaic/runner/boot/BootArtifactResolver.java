package org.mosaic.runner.boot;

import org.mosaic.runner.ServerHome;

/**
 * @author arik
 */
public interface BootArtifactResolver {

    void resolve( ServerHome home, BootArtifact artifact ) throws CannotInstallBootArtifactException;

}
