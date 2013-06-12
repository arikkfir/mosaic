package org.mosaic.lifecycle.impl;

import com.google.common.reflect.TypeToken;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import javax.annotation.Nonnull;
import org.mosaic.lifecycle.Module;
import org.mosaic.lifecycle.ModuleManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.springframework.util.ReflectionUtils;

/**
 * @author arik
 */
public class ServiceExportImpl implements Module.ServiceExport
{
    @Nonnull
    private final ModuleManager moduleManager;

    @Nonnull
    private final Module module;

    @Nonnull
    private final TypeToken<?> type;

    @Nonnull
    private final ServiceRegistration<?> serviceRegistration;

    @Nonnull
    private final ServiceReference<?> serviceReference;

    public ServiceExportImpl( @Nonnull ModuleManager moduleManager,
                              @Nonnull Module module,
                              @Nonnull TypeToken<?> type,
                              @Nonnull ServiceReference<?> serviceReference )
    {
        this.moduleManager = moduleManager;
        this.module = module;
        this.type = type;
        this.serviceReference = serviceReference;

        Method getRegistrationMethod = ReflectionUtils.findMethod( serviceReference.getClass(), "getRegistration" );
        getRegistrationMethod.setAccessible( true );
        try
        {
            this.serviceRegistration = ( ServiceRegistration<?> ) getRegistrationMethod.invoke( serviceReference );
        }
        catch( IllegalAccessException | InvocationTargetException e )
        {
            throw new IllegalStateException( "Could not obtain OSGi service registration from a service reference '" + serviceReference + "':" + e.getMessage(), e );
        }
    }

    public ServiceExportImpl( @Nonnull ModuleManager moduleManager,
                              @Nonnull Module module,
                              @Nonnull TypeToken<?> type,
                              @Nonnull ServiceRegistration<?> serviceRegistration )
    {
        this.moduleManager = moduleManager;
        this.module = module;
        this.type = type;
        this.serviceRegistration = serviceRegistration;
        this.serviceReference = serviceRegistration.getReference();
    }

    @Nonnull
    @Override
    public Module getProvider()
    {
        return this.module;
    }

    @Nonnull
    @Override
    public TypeToken<?> getType()
    {
        return this.type;
    }

    @Nonnull
    @Override
    public Map<String, Object> getProperties()
    {
        Map<String, Object> properties = new LinkedHashMap<>( 10 );
        for( String key : this.serviceReference.getPropertyKeys() )
        {
            properties.put( key, this.serviceReference.getProperty( key ) );
        }
        return properties;
    }

    @Nonnull
    @Override
    public Collection<Module> getConsumers()
    {
        Collection<Module> modules = new LinkedList<>();
        Bundle[] usingBundles = this.serviceReference.getUsingBundles();
        if( usingBundles != null )
        {
            for( Bundle usingBundle : usingBundles )
            {
                Module module = this.moduleManager.getModule( usingBundle.getBundleId() );
                if( module != null )
                {
                    modules.add( module );
                }
            }
        }
        return modules;
    }

    @Override
    public boolean isRegistered()
    {
        return this.serviceReference.getBundle() != null;
    }

    @Override
    public void unregister()
    {
        this.serviceRegistration.unregister();
    }
}
