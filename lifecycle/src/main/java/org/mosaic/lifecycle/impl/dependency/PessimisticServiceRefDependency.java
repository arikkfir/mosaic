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
public class PessimisticServiceRefDependency extends OptimisticServiceRefDependency
{
    public PessimisticServiceRefDependency( @Nonnull ModuleImpl module,
                                            @Nullable String filterSpec,
                                            boolean required,
                                            @Nonnull String beanName,
                                            @Nonnull MethodHandle methodHandle )
    {
        super( module, filterSpec, required, beanName, methodHandle );
    }

    @Override
    public boolean isSatisfiedInternal( @Nonnull ServiceTracker<?, ?> tracker )
    {
        return !this.required || tracker.getTrackingCount() == 1;
    }

    @Override
    protected void onServiceAdded( @Nonnull ServiceReference<Object> reference, @Nonnull Object service )
    {
        if( this.injectedServiceInstance == null )
        {
            super.onServiceAdded( reference, service );
        }
        else
        {
            // we now track more than one service and we are not allowed to auto-choose - deactivate
            this.module.deactivate();
        }
    }
}
