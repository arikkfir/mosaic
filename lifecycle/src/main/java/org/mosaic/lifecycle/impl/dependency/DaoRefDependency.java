package org.mosaic.lifecycle.impl.dependency;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.sql.DataSource;
import org.mosaic.database.dao.DaoFactory;
import org.mosaic.lifecycle.ModuleState;
import org.mosaic.lifecycle.impl.ActivationReason;
import org.mosaic.lifecycle.impl.ModuleImpl;
import org.mosaic.util.reflection.MethodHandle;
import org.mosaic.util.reflection.MethodParameter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mosaic.lifecycle.impl.util.FilterUtils.createFilter;

/**
 * @author arik
 */
public class DaoRefDependency extends AbstractBeanDependency
{
    private static final Logger LOG = LoggerFactory.getLogger( DaoRefDependency.class );

    @Nonnull
    protected final ModuleImpl module;

    @Nonnull
    protected final String beanName;

    @Nonnull
    protected final Class<?> daoType;

    @Nullable
    protected final String dataSourceName;

    @Nonnull
    protected final MethodHandle methodHandle;

    @Nonnull
    protected final MethodHandle.Invoker invoker;

    @Nullable
    protected ServiceTracker<DaoFactory, DaoFactory> daoFactoryTracker;

    @Nullable
    protected ServiceTracker<DataSource, DataSource> dataSourceTracker;

    @Nullable
    private DaoFactory daoFactory;

    @Nullable
    private Object dao;

    @Nullable
    private DataSource dataSource;

    public DaoRefDependency( @Nonnull ModuleImpl module,
                             @Nonnull String beanName,
                             @Nonnull MethodHandle methodHandle,
                             @Nullable String dataSourceName )
    {
        this.module = module;
        this.beanName = beanName;
        this.methodHandle = methodHandle;
        this.dataSourceName = dataSourceName;

        List<MethodParameter> parameters = methodHandle.getParameters();
        if( parameters.size() != 1 )
        {
            throw new IllegalArgumentException( "@Dao method '" + methodHandle + "' has " + parameters.size() + " parameters - but must only have 1" );
        }
        this.daoType = parameters.get( 0 ).getType().getRawType();
        this.invoker = this.methodHandle.createInvoker( new ServiceInstanceResolver() );
    }

    @Override
    public String toString()
    {
        return String.format( "Dao[%s] using data source '%s' for %s",
                              this.daoType.getSimpleName(),
                              this.dataSourceName,
                              this.methodHandle );
    }

    @Nonnull
    public final String getBeanName()
    {
        return this.beanName;
    }

    @Override
    public void start()
    {
        this.daoFactoryTracker = new ServiceTracker<>( this.module.getBundleContext(), DaoFactory.class, new DaoFactoryCustomizer() );
        this.daoFactoryTracker.open();

        String dsFilterString = this.dataSourceName == null ? null : "(name=" + this.dataSourceName + ")";
        Filter dataSourceFilter = createFilter( DataSource.class, dsFilterString );
        this.dataSourceTracker = new ServiceTracker<>( this.module.getBundleContext(), dataSourceFilter, new DataSourceCustomizer() );
        this.dataSourceTracker.open();
    }

    @Override
    public boolean isSatisfied()
    {
        return this.daoFactoryTracker != null && !this.daoFactoryTracker.isEmpty();
    }

    @Override
    public void stop()
    {
        if( this.daoFactoryTracker != null )
        {
            this.daoFactoryTracker.close();
            this.daoFactoryTracker = null;
        }
    }

    @Override
    public void beanCreated( @Nonnull Object bean )
    {
        if( this.daoFactoryTracker != null && !this.daoFactoryTracker.isEmpty() && this.dataSourceTracker != null && !this.dataSourceTracker.isEmpty() )
        {
            this.daoFactory = this.daoFactoryTracker.getService();
            this.dataSource = this.dataSourceTracker.getService();
            this.dao = this.daoFactory.create( this.daoType, this.dataSource );
            inject( bean, this.dao );
        }
    }

    @Override
    public void beanInitialized( @Nonnull Object bean )
    {
        // no-op
    }

    protected final void inject( @Nullable Object dao )
    {
        Object bean = this.module.getBean( this.beanName );
        if( bean != null )
        {
            inject( bean, dao );
        }
    }

    protected final void inject( @Nonnull Object bean, @Nullable Object dao )
    {
        Map<String, Object> context = new HashMap<>();
        context.put( "service", dao );
        try
        {
            this.invoker.resolve( context ).invoke( bean );
        }
        catch( Exception e )
        {
            LOG.error( "Could not inject DAO '{}' to method '{}' in bean '{}' of module '{}': {}",
                       dao, this.methodHandle, bean, this.module, e.getMessage(), e );
        }
    }

    private void attemptInjection()
    {
        if( this.daoFactory != null && this.dataSource != null )
        {
            if( module.getState() == ModuleState.ACTIVE )
            {
                this.dao = this.daoFactory.create( this.daoType, this.dataSource );
                inject( this.dao );
            }
            else
            {
                module.activateIfReady( ActivationReason.DEPENDENCY_SATISFIED );
            }
        }
        else
        {
            this.module.deactivate();
            this.dao = null;
            this.dataSource = null;
            this.daoFactory = null;
        }
    }

    private class DataSourceCustomizer implements ServiceTrackerCustomizer<DataSource, DataSource>
    {
        @Override
        public DataSource addingService( ServiceReference<DataSource> reference )
        {
            DataSource service = null;

            BundleContext bundleContext = module.getBundleContext();
            if( bundleContext != null )
            {
                service = bundleContext.getService( reference );
                if( service != null && dataSource == null )
                {
                    dataSource = service;
                    attemptInjection();
                }
            }

            return service;
        }

        @Override
        public void modifiedService( ServiceReference<DataSource> reference, DataSource service )
        {
            // no-op
        }

        @Override
        public void removedService( ServiceReference<DataSource> reference, DataSource service )
        {
            if( service == dataSource && dataSourceTracker != null )
            {
                dataSource = dataSourceTracker.getService();
                attemptInjection();
            }
        }
    }

    private class DaoFactoryCustomizer implements ServiceTrackerCustomizer<DaoFactory, DaoFactory>
    {
        @Override
        public DaoFactory addingService( ServiceReference<DaoFactory> reference )
        {
            DaoFactory service = null;

            BundleContext bundleContext = module.getBundleContext();
            if( bundleContext != null )
            {
                service = bundleContext.getService( reference );
                if( service != null && daoFactory == null )
                {
                    daoFactory = service;
                    attemptInjection();
                }
            }

            return service;
        }

        @Override
        public void modifiedService( ServiceReference<DaoFactory> reference, DaoFactory service )
        {
            // no-op
        }

        @Override
        public void removedService( ServiceReference<DaoFactory> reference, DaoFactory service )
        {
            if( service == daoFactory && daoFactoryTracker != null )
            {
                daoFactory = daoFactoryTracker.getService();
                dao = null;
                attemptInjection();
            }
        }
    }
}
