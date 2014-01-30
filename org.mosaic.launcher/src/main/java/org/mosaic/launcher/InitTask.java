package org.mosaic.launcher;

import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
abstract class InitTask
{
    @Nonnull
    final Logger log = LoggerFactory.getLogger( getClass() );

    abstract void start();

    abstract void stop();
}
