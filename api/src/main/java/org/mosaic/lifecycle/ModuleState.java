package org.mosaic.lifecycle;

/**
 * @author arik
 */
public enum ModuleState
{
    UNKNOWN,
    INSTALLED,
    RESOLVED,
    STARTING,
    STARTED,
    ACTIVATING,
    ACTIVE,
    DEACTIVATING,
    STOPPING,
    UNINSTALLED
}
