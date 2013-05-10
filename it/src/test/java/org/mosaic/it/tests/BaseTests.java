package org.mosaic.it.tests;

import java.io.IOException;
import org.junit.Assert;
import org.mosaic.it.runner.MosaicRunner;

/**
 * @author arik
 */
public abstract class BaseTests extends Assert
{
    protected final MosaicRunner runner() throws IOException
    {
        return new MosaicRunner().onDevelopmentMode();
    }
}
