package org.mosaic.filewatch;

/**
 * @author arik
 */
public enum WatchEvent
{
    SCAN_STARTING,
    DIR_ENTER,
    FILE_ADDED,
    FILE_MODIFIED,
    FILE_DELETED,
    DIR_EXIT,
    SCAN_FINISHED
}
