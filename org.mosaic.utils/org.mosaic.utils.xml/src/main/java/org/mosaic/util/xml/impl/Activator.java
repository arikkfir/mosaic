package org.mosaic.util.xml.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.xml.XmlParser;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * @author arik
 */
public final class Activator implements BundleActivator
{
    @Nullable
    private ServiceRegistration<XmlParser> registration;

    @Override
    public void start( @Nonnull BundleContext context ) throws Exception
    {
        this.registration = context.registerService( XmlParser.class, new XmlParserImpl(), null );
    }

    @Override
    public void stop( @Nonnull BundleContext context ) throws Exception
    {
        ServiceRegistration<XmlParser> registration = this.registration;
        if( registration != null )
        {
            try
            {
                registration.unregister();
            }
            catch( Exception ignore )
            {
            }
            this.registration = null;
        }
    }
}
