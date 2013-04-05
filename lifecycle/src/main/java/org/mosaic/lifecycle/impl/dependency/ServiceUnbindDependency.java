package org.mosaic.lifecycle.impl.dependency;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.lifecycle.impl.ModuleImpl;
import org.mosaic.util.reflection.MethodHandle;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 * @author arik
 */
public class ServiceUnbindDependency extends AbstractSingleServiceDependency
{
    public ServiceUnbindDependency( @Nonnull ModuleImpl module,
                                    @Nullable String filterSpec,
                                    @Nonnull String beanName,
                                    @Nonnull MethodHandle methodHandle )
    {
        super( module, filterSpec, beanName, methodHandle );
    }

    @Override
    public boolean isSatisfiedInternal( @Nonnull ServiceTracker<?, ?> tracker )
    {
        return true;
    }

    public void beanCreated( @Nonnull Object bean )
    {
        // no-op
    }

    @Override
    public void beanInitialized( @Nonnull Object bean )
    {
        // no-op
    }

    @Override
    protected void onServiceRemoved( @Nonnull ServiceReference<Object> reference, @Nullable Object service )
    {
        inject( reference, service );
    }
}
