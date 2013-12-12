package org.mosaic.util.impl;

import org.mosaic.util.conversion.impl.ConversionActivator;
import org.mosaic.util.expression.impl.ExpressionParserActivator;
import org.mosaic.util.osgi.BundleActivatorAggregate;
import org.mosaic.util.reflection.impl.MethodHandleFactoryActivator;
import org.mosaic.util.resource.impl.PathWatcherManagerActivator;
import org.mosaic.util.xml.impl.XmlParserActivator;

/**
 * @author arik
 */
public class Activator extends BundleActivatorAggregate
{
    public Activator()
    {
        addTarget( new ConversionActivator() );
        addTarget( new ExpressionParserActivator() );
        addTarget( new MethodHandleFactoryActivator() );
        addTarget( new XmlParserActivator() );
        addTarget( new PathWatcherManagerActivator() );
    }
}
