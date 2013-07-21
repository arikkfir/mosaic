package org.mosaic.lifecycle.impl.dependency;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.lifecycle.ModuleState;
import org.mosaic.lifecycle.impl.ModuleImpl;
import org.mosaic.util.reflection.MethodHandle;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import static org.mosaic.lifecycle.impl.util.FilterUtils.createFilter;

/**
 * @author arik
 */
public class OptimisticServiceRefDependency extends AbstractSingleServiceDependency
{
    protected final boolean required;

    protected Object injectedServiceInstance;

    private ServiceReference<?> temporarySatisfyingService;

    public OptimisticServiceRefDependency( @Nonnull ModuleImpl module,
                                           @Nullable String filterSpec,
                                           boolean required,
                                           @Nonnull String beanName,
                                           @Nonnull MethodHandle methodHandle )
    {
        super( module, filterSpec, beanName, methodHandle );
        this.required = required;
    }

    @Override
    public String toString()
    {
        if( this.serviceType == null )
        {
            return String.format( "ServiceRef[unknown] for %s", this.methodHandle );
        }
        else
        {
            return String.format( "ServiceRef[%s] for %s",
                                  createFilter( this.serviceType, this.filter ),
                                  this.methodHandle );
        }
    }

    @Override
    public void start()
    {
        this.injectedServiceInstance = null;
        super.start();
    }

    @Override
    public boolean isSatisfiedInternal( @Nonnull ServiceTracker<?, ?> tracker )
    {
        return this.temporarySatisfyingService != null || !this.required || !tracker.isEmpty();
    }

    @Override
    public void stop()
    {
        super.stop();
        this.injectedServiceInstance = null;
    }

    public void beanCreated( @Nonnull Object bean )
    {
        if( this.tracker != null )
        {
            ServiceReference<?> reference;
            Object service;

            if( this.temporarySatisfyingService != null )
            {
                reference = this.temporarySatisfyingService;
                service = this.module.getBundle().getBundleContext().getService( this.temporarySatisfyingService );
            }
            else
            {
                reference = this.tracker.getServiceReference();
                service = this.tracker.getService();
            }
            inject( bean, reference, service );
            this.injectedServiceInstance = service;
        }
    }

    @Override
    public void beanInitialized( @Nonnull Object bean )
    {
        // no-op
    }

    @Override
    protected void onServiceAdded( @Nonnull ServiceReference<Object> reference, @Nonnull Object service )
    {
        if( this.injectedServiceInstance != service )
        {
            if( this.module.getState() == ModuleState.ACTIVE )
            {
                inject( reference, service );
                this.injectedServiceInstance = service;
            }
            else
            {
                this.temporarySatisfyingService = reference;
                try
                {
                    this.module.onDependencySatisfied();
                }
                finally
                {
                    this.temporarySatisfyingService = null;
                }
            }
        }
    }

    @Override
    protected void onServiceRemoved( @Nonnull ServiceReference<Object> reference, @Nullable Object service )
    {
        if( service == this.injectedServiceInstance && this.tracker != null )
        {
            Object replacement = this.tracker.getService();
            if( replacement != null || !this.required )
            {
                if( this.module.getState() == ModuleState.ACTIVE )
                {
                    inject( reference, replacement );
                    this.injectedServiceInstance = service;
                }
                else
                {
                    this.module.onDependencySatisfied();
                }
            }
            else
            {
                this.module.onDependencyUnsatisfied();
                this.injectedServiceInstance = null;
            }
        }
    }
}
