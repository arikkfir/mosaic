package org.mosaic.util.convert.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.lifecycle.impl.util.ServiceUtils;
import org.mosaic.util.convert.Converter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import static java.lang.Boolean.parseBoolean;

/**
 * @author arik
 */
public class StringToBooleanConverter implements Converter<String, Boolean>, InitializingBean, DisposableBean
{
    @Nonnull
    private final BundleContext bundleContext;

    @Nullable
    private ServiceRegistration<Converter> registration;

    public StringToBooleanConverter( @Nonnull BundleContext bundleContext )
    {
        this.bundleContext = bundleContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        this.registration = ServiceUtils.register( this.bundleContext, Converter.class, this );
    }

    @Override
    public void destroy() throws Exception
    {
        this.registration = ServiceUtils.unregister( this.registration );
    }

    @Nonnull
    @Override
    public Boolean convert( @Nonnull String s )
    {
        return "yes".equalsIgnoreCase( s ) || "on".equalsIgnoreCase( s ) || parseBoolean( s );
    }
}
