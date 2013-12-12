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
public final class XmlParserActivator implements BundleActivator
{
    @Nullable
    private ServiceRegistration<XmlParser> xmlParserServiceRegistration;

    @Override
    public void start( @Nonnull final BundleContext bundleContext ) throws Exception
    {
        this.xmlParserServiceRegistration = bundleContext.registerService( XmlParser.class, new XmlParserImpl(), null );
    }

    @Override
    public void stop( @Nonnull BundleContext context ) throws Exception
    {
        if( this.xmlParserServiceRegistration != null )
        {
            try
            {
                this.xmlParserServiceRegistration.unregister();
            }
            catch( Exception ignore )
            {
            }
        }
        this.xmlParserServiceRegistration = null;
    }
}
