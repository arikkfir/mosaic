package org.mosaic.lifecycle.impl.dependency;

/**
 * @author arik
 */
public abstract class AbstractDependency
{
    public abstract void start();

    public abstract boolean isSatisfied();

    public abstract void stop();
}
