package org.mosaic.core.launcher.impl;

import java.io.IOException;
import org.mosaic.core.util.Nonnull;
import org.osgi.framework.BundleException;

/**
 * @author arik
 */
public class Main
{
    public static void main( @Nonnull String... args ) throws IOException, BundleException
    {
        new ServerImpl();
    }
}
