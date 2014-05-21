package org.mosaic.core.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import org.mosaic.core.Component;
import org.mosaic.core.Module;
import org.mosaic.core.OnDeactivation;
import org.mosaic.core.ServiceRegistration;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.core.util.base.ToStringHelper;
import org.slf4j.Logger;

import static java.util.Collections.unmodifiableList;

/**
 * @author arik
 */
class ModuleComponentImpl
{
    @Nonnull
    private final ModuleTypeImpl moduleType;

    @Nonnull
    private final List<ProvidedType> providedTypes;

    @Nullable
    private final Callable<Object> instantiator;

    @Nonnull
    private final List<Method> deactivationMethods;

    @Nullable
    private Object instance;

    ModuleComponentImpl( @Nonnull ModuleTypeImpl moduleType, List<Component> annotations )
    {
        this.moduleType = moduleType;
        this.instantiator = createInstantiator( this.moduleType.getType() );

        List<ProvidedType> providedTypes = new LinkedList<>();
        for( Component annotation : annotations )
        {
            if( !void.class.equals( annotation.value() ) )
            {
                providedTypes.add( new ProvidedType( annotation ) );
            }
        }
        this.providedTypes = unmodifiableList( providedTypes );

        Logger logger = this.moduleType.getModuleRevision().getModule().getServer().getLogger();

        List<Method> deactivationMethods = new LinkedList<>();
        Class<?> type = this.moduleType.getType();
        while( type != null && !type.getPackage().getName().startsWith( "java." ) && !type.getPackage().getName().startsWith( "javax." ) )
        {
            for( Method method : this.moduleType.getType().getDeclaredMethods() )
            {
                if( method.isAnnotationPresent( OnDeactivation.class ) )
                {
                    if( method.getParameterTypes().length > 0 )
                    {
                        logger.warn( "@OnDeactivation methods must not have parameters (found in method '{}' of type '{}')", method.toGenericString(), this.moduleType );
                    }
                    else
                    {
                        deactivationMethods.add( method );
                    }
                }
            }
            type = type.getSuperclass();
        }
        this.deactivationMethods = unmodifiableList( deactivationMethods );
    }

    @Override
    public String toString()
    {
        return ToStringHelper.create( this )
                             .add( "type", this.moduleType )
                             .add( "provides", this.providedTypes )
                             .toString();
    }

    void activate()
    {
        ModuleImpl module = this.moduleType.getModuleRevision().getModule();
        Logger logger = module.getServer().getLogger();

        if( this.instantiator != null )
        {
            module.getServer().getLogger().debug( "Activating component {}", this );
            try
            {
                this.instance = this.instantiator.call();
            }
            catch( Exception e )
            {
                logger.error( "Error activating component {} - instantiation error", this, e );
                return;
            }

            for( ProvidedType providedType : this.providedTypes )
            {
                providedType.register( this.instance );
            }
        }
    }

    void deactivate()
    {
        Object instance = this.instance;
        if( instance != null )
        {
            Logger logger = this.moduleType.getModuleRevision().getModule().getServer().getLogger();

            logger.debug( "Deactivating component {}", this );
            for( ProvidedType providedType : this.providedTypes )
            {
                providedType.unregister();
            }

            for( Method method : this.deactivationMethods )
            {
                try
                {
                    method.invoke( instance );
                }
                catch( Throwable e )
                {
                    logger.warn( "@OnDeactivation method '{}' of component {} threw an exception",
                                 method.toGenericString(), this, e );
                }
            }
            this.instance = null;
        }
    }

    @Nullable
    private Callable<Object> createInstantiator( @Nonnull Class<?> type )
    {
        try
        {
            Method getInstanceMethod = type.getDeclaredMethod( "getInstance" );
            if( Modifier.isStatic( getInstanceMethod.getModifiers() ) )
            {
                getInstanceMethod.setAccessible( true );
                return new FactoryMethodInstantiator( getInstanceMethod );
            }
        }
        catch( NoSuchMethodException ignore )
        {
        }
        catch( Throwable e )
        {
            this.moduleType.getModuleRevision().getModule().getServer().getLogger().warn( "Error discovering factory method for {}", this, e );
        }

        try
        {
            Constructor<?> constructor = type.getDeclaredConstructor();
            constructor.setAccessible( true );
            return new ConstructorInstantiator( constructor );
        }
        catch( NoSuchMethodException ignore )
        {
        }
        catch( Throwable e )
        {
            this.moduleType.getModuleRevision().getModule().getServer().getLogger().warn( "Error discovering constructor for {}", this, e );
        }

        return null;
    }

    private class ProvidedType
    {
        @Nonnull
        private final Class type;

        @Nonnull
        private final Module.ServiceProperty[] properties;

        @Nullable
        private ServiceRegistration registration;

        private ProvidedType( @Nonnull Component annotation )
        {
            this.type = annotation.value();
            List<Module.ServiceProperty> properties = new LinkedList<>();
            for( Component.Property property : annotation.properties() )
            {
                properties.add( Module.ServiceProperty.p( property.name(), property.value() ) );
            }
            this.properties = properties.toArray( new Module.ServiceProperty[ properties.size() ] );
        }

        @Override
        public String toString()
        {
            return ToStringHelper.create( this )
                                 .add( "type", this.type.getName() )
                                 .toString();
        }

        @SuppressWarnings( "unchecked" )
        private void register( @Nonnull Object instance )
        {
            ModuleImpl module = ModuleComponentImpl.this.moduleType.getModuleRevision().getModule();
            this.registration = module.registerService( this.type, instance, this.properties );
        }

        private void unregister()
        {
            ServiceRegistration registration = this.registration;
            if( registration != null )
            {
                registration.unregister();
                this.registration = null;
            }
        }
    }

    private class ConstructorInstantiator implements Callable<Object>
    {
        @Nonnull
        private final Constructor<?> constructor;

        private ConstructorInstantiator( @Nonnull Constructor<?> constructor )
        {
            this.constructor = constructor;
        }

        @Override
        public String toString()
        {
            return ToStringHelper.create( this )
                                 .add( "constructor", this.constructor.toGenericString() )
                                 .toString();
        }

        @Override
        public Object call() throws Exception
        {
            ModuleImpl module = ModuleComponentImpl.this.moduleType.getModuleRevision().getModule();
            Logger logger = module.getServer().getLogger();
            try
            {
                this.constructor.setAccessible( true );
                return this.constructor.newInstance();
            }
            catch( Throwable e )
            {
                logger.error( "Error activating component {} - instantiation error", this, e );
                return null;
            }
        }
    }

    private class FactoryMethodInstantiator implements Callable<Object>
    {
        @Nonnull
        private final Method method;

        private FactoryMethodInstantiator( @Nonnull Method method )
        {
            this.method = method;
        }

        @Override
        public String toString()
        {
            return ToStringHelper.create( this )
                                 .add( "method", this.method.toGenericString() )
                                 .toString();
        }

        @Override
        public Object call() throws Exception
        {
            ModuleImpl module = ModuleComponentImpl.this.moduleType.getModuleRevision().getModule();
            Logger logger = module.getServer().getLogger();
            try
            {
                this.method.setAccessible( true );
                return this.method.invoke( null );
            }
            catch( Throwable e )
            {
                logger.error( "Error activating component {} - instantiation error", this, e );
                return null;
            }
        }
    }
}
