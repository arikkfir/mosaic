package org.mosaic.it.tests;

import java.io.IOException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.mosaic.it.runner.MosaicRunner;

/**
 * @author arik
 */
public abstract class BaseTests extends Assert
{
    protected MosaicRunner runner;

    @Before
    public void setupMosaicRunner() throws IOException
    {
        this.runner = new MosaicRunner();
    }

    @After
    public void tearDownMosaicRunner() throws IOException
    {
        this.runner.stop();
        this.runner = null;
    }
}
