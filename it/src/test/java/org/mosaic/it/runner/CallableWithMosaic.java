package org.mosaic.it.runner;

import javax.annotation.Nonnull;

/**
 * @author arik
 */
public interface CallableWithMosaic<T>
{
    T run( @Nonnull MosaicRunner runner ) throws Exception;
}
