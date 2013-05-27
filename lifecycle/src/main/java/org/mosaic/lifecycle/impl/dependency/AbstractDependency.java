package org.mosaic.lifecycle.impl.dependency;

import org.mosaic.lifecycle.Module;

/**
 * @author arik
 */
public abstract class AbstractDependency implements Module.Dependency
{
    public abstract void start();

    public abstract void stop();
}
