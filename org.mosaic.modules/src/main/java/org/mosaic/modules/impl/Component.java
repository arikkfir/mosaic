package org.mosaic.modules.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import javax.annotation.PreDestroy;
import org.mosaic.modules.*;
import org.mosaic.modules.spi.MethodEndpointMarker;

import static org.mosaic.util.reflection.MethodAnnotations.getMetaAnnotationTarget;

/**
 * @author arik
 */
class Component extends Lifecycle
{
    @Nonnull
    private final TypeDescriptor typeDescriptor;

    @Nullable
    private Object instance;

    Component( @Nonnull TypeDescriptor typeDescriptor )
    {
        this.typeDescriptor = typeDescriptor;

        // can this type be a @Component?
        int modifiers = this.typeDescriptor.getType().getModifiers();
        if( Modifier.isAbstract( modifiers ) || Modifier.isInterface( modifiers ) )
        {
            throw new ComponentDefinitionException( "type " + this.typeDescriptor + " cannot be a @Component - it is abstract or an interface",
                                                    this.typeDescriptor.getType(), this.typeDescriptor.getModule() );
        }

        // is this a @Service?
        if( this.typeDescriptor.getType().isAnnotationPresent( Service.class ) )
        {
            addChild( new ComponentServiceExporter( this ) );
        }

        // collect @PreDestroy, @OnService(Added|Removed) and @MethodEndpointMarker methods
        Class<?> type = this.typeDescriptor.getType();
        while( type != null )
        {
            for( Method method : type.getDeclaredMethods() )
            {
                if( method.isAnnotationPresent( PreDestroy.class ) )
                {
                    addChild( new ComponentPreDestroyMethod( this, method ) );
                }
                else if( method.isAnnotationPresent( OnServiceAdded.class ) || method.isAnnotationPresent( OnServiceRemoved.class ) )
                {
                    addChild( new ComponentServiceEventMethod( this, method ) );
                }

                Annotation annotation = getMetaAnnotationTarget( method, MethodEndpointMarker.class );
                if( annotation != null )
                {
                    addChild( new ComponentMethodEndpoint( this, method, annotation ) );
                }
            }
            type = type.getSuperclass();
        }

    }

    @Override
    public final String toString()
    {
        return "Component[" + this.typeDescriptor + "]";
    }

    @Override
    protected synchronized void onBeforeActivate()
    {
        try
        {
            Constructor<?> defaultConstructor = this.typeDescriptor.getType().getDeclaredConstructor();
            defaultConstructor.setAccessible( true );
            this.instance = defaultConstructor.newInstance();
        }
        catch( Exception e )
        {
            throw new ComponentCreateException( e, this.typeDescriptor.getType(), this.typeDescriptor.getModule() );
        }
    }

    @Override
    protected synchronized void onAfterDeactivate()
    {
        this.instance = null;
    }

    @Nonnull
    ModuleImpl getModule()
    {
        return this.typeDescriptor.getModule();
    }

    @Nonnull
    Class<?> getType()
    {
        return this.typeDescriptor.getType();
    }

    @Nullable
    Object getInstance()
    {
        return instance;
    }
}
