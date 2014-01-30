package org.mosaic.modules.impl;

import com.google.common.base.Optional;
import java.lang.reflect.ParameterizedType;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Dictionary;
import java.util.Hashtable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import org.mosaic.modules.ComponentDefinitionException;
import org.mosaic.modules.MethodEndpoint;
import org.mosaic.modules.ModuleManager;
import org.mosaic.modules.ServiceTemplate;
import org.mosaic.util.osgi.BundleUtils;
import org.mosaic.util.osgi.FilterBuilder;
import org.mosaic.util.resource.PathWatcher;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.weaving.WeavingHook;

/**
 * @author arik
 */
public final class Activator implements BundleActivator
{
    @Nullable
    private static ModuleManagerImpl moduleManager;

    @Nonnull
    public static ModuleManager getModuleManager()
    {
        ModuleManagerImpl moduleManager = Activator.moduleManager;
        if( moduleManager == null )
        {
            throw new IllegalStateException( "module manager is not available" );
        }
        else
        {
            return moduleManager;
        }
    }

    @Nonnull
    static Path getWorkPath()
    {
        Optional<BundleContext> bcHolder = BundleUtils.bundleContext( Activator.class );
        String workPath = bcHolder.get().getProperty( "mosaic.home.work" );
        if( workPath == null )
        {
            throw new IllegalStateException( "bundle property 'mosaic.home.work' is missing" );
        }
        return Paths.get( workPath );
    }

    @Nonnull
    static Pair<Class<?>, FilterBuilder> getServiceAndFilterFromType( @Nonnull ModuleImpl module,
                                                                      @Nonnull Class<?> componentType,
                                                                      @Nonnull java.lang.reflect.Type type )
    {
        if( type instanceof Class<?> )
        {
            return Pair.<Class<?>, FilterBuilder>of( ( Class<?> ) type, new FilterBuilder().addClass( ( Class<?> ) type ) );
        }
        else if( type instanceof ParameterizedType )
        {
            ParameterizedType parameterizedType = ( ParameterizedType ) type;
            java.lang.reflect.Type rawType = parameterizedType.getRawType();

            if( !MethodEndpoint.class.equals( rawType ) && !ServiceTemplate.class.equals( rawType ) )
            {
                String msg = "only MethodEndpoint and ServiceTemplate can serve as parameterized service types";
                throw new ComponentDefinitionException( msg, componentType, module );
            }

            FilterBuilder filterBuilder = new FilterBuilder().addClass( ( Class<?> ) rawType );

            java.lang.reflect.Type[] typeArguments = parameterizedType.getActualTypeArguments();
            if( typeArguments.length == 1 )
            {
                java.lang.reflect.Type arg = typeArguments[ 0 ];
                if( arg instanceof Class<?> )
                {
                    filterBuilder.addEquals( "type", ( ( Class<?> ) arg ).getName() );
                }
                else
                {
                    String msg = "MethodEndpoint can only receive concrete type arguments";
                    throw new ComponentDefinitionException( msg, componentType, module );
                }
            }
            return Pair.<Class<?>, FilterBuilder>of( MethodEndpoint.class, filterBuilder );
        }
        else
        {
            String msg = "illegal service type: " + type;
            throw new ComponentDefinitionException( msg, componentType, module );
        }
    }

    @Nullable
    private ServiceRegistration<WeavingHook> weavingHookServiceRegistration;

    @Nullable
    private ServiceRegistration<ModuleManager> moduleManagerServiceRegistration;

    @Nullable
    private ServiceRegistration<PathWatcher> libWatcherServiceRegistration;

    @Override
    public void start( @Nonnull final BundleContext context ) throws Exception
    {
        this.weavingHookServiceRegistration = context.registerService( WeavingHook.class, new ModuleWeavingHook( context ), null );

        moduleManager = new ModuleManagerImpl();
        moduleManager.open( context );
        this.moduleManagerServiceRegistration = context.registerService( ModuleManager.class, moduleManager, null );

        new Thread( new Runnable()
        {
            @Override
            public void run()
            {
                Dictionary<String, Object> dict = new Hashtable<>();
                dict.put( "location", "${mosaic.home.lib}" );
                Activator.this.libWatcherServiceRegistration = context.registerService( PathWatcher.class, new ServerLibWatcher(), dict );
            }
        }, "ModulesStarter" ).start();
    }

    @Override
    public void stop( @Nonnull BundleContext context ) throws Exception
    {
        ServiceRegistration<PathWatcher> libWatcherServiceRegistration = this.libWatcherServiceRegistration;
        if( libWatcherServiceRegistration != null )
        {
            try
            {
                libWatcherServiceRegistration.unregister();
            }
            catch( Exception ignore )
            {
            }
            this.libWatcherServiceRegistration = null;
        }

        ModuleManagerImpl moduleManager = Activator.moduleManager;
        if( moduleManager != null )
        {
            moduleManager.close( context );
            Activator.moduleManager = null;
        }

        ServiceRegistration<WeavingHook> weavingHookServiceRegistration = this.weavingHookServiceRegistration;
        if( weavingHookServiceRegistration != null )
        {
            try
            {
                weavingHookServiceRegistration.unregister();
            }
            catch( Exception ignore )
            {
            }
            this.weavingHookServiceRegistration = null;
        }

        ServiceRegistration<ModuleManager> moduleManagerServiceRegistration = this.moduleManagerServiceRegistration;
        if( moduleManagerServiceRegistration != null )
        {
            try
            {
                moduleManagerServiceRegistration.unregister();
            }
            catch( Exception ignore )
            {
            }
            this.moduleManagerServiceRegistration = null;
        }
    }
}
