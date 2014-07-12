package org.mosaic.it.tests.core;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mosaic.core.components.Inject;
import org.mosaic.core.launcher.impl.ServerImpl;
import org.mosaic.core.modules.ModuleRevision;
import org.mosaic.core.util.Nonnull;
import org.mosaic.it.runner.MosaicRunner;
import org.mosaic.it.runner.WithServer;

/**
 * @author arik
 */
@RunWith(MosaicRunner.class)
public class SimpleTests
{
    @Nonnull
    @Inject
    private ServerImpl server;

    @Test
    @WithServer(modules = { "it01", "org.mosaic.convert" })
    public void simpleTest()
    {
        boolean it01Deployed = this.server.getModuleManager()
                                          .getModules()
                                          .stream()
                                          .filter( module -> {
                                              ModuleRevision revision = module.getCurrentRevision();
                                              return revision != null && revision.getName().equals( "org.mosaic.it.modules.it01" );
                                          } )
                                          .findFirst().isPresent();
        Assert.assertTrue( "it01 module not deployed", it01Deployed );
    }
}
