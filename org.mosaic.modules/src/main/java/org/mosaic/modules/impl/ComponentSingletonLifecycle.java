package org.mosaic.modules.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PreDestroy;
import org.mosaic.modules.ComponentCreateException;
import org.mosaic.modules.OnServiceAdded;
import org.mosaic.modules.OnServiceRemoved;
import org.mosaic.modules.Service;
import org.mosaic.modules.spi.MethodEndpointMarker;
import org.mosaic.util.reflection.AnnotationFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
final class ComponentSingletonLifecycle extends Lifecycle
{
    private static final Logger LOG = LoggerFactory.getLogger( ComponentSingletonLifecycle.class );

    @Nonnull
    private final ComponentDescriptorImpl<?> componentDescriptor;

    @Nonnull
    private final List<Method> preDestroyMethods;

    @Nullable
    private Object instance;

    ComponentSingletonLifecycle( @Nonnull ComponentDescriptorImpl<?> componentDescriptor,
                                 @Nonnull Class<?> componentType )
    {
        this.componentDescriptor = componentDescriptor;
        this.preDestroyMethods = new LinkedList<>();

        Class<?> type = componentType;
        while( type != null )
        {
            for( Method method : type.getDeclaredMethods() )
            {
                if( method.isAnnotationPresent( PreDestroy.class ) )
                {
                    this.preDestroyMethods.add( method );
                    method.setAccessible( true );
                }
                else if( method.isAnnotationPresent( OnServiceAdded.class ) || method.isAnnotationPresent( OnServiceRemoved.class ) )
                {
                    addChild( new ComponentServiceEventMethodLifecycle( componentDescriptor, method ) );
                }

                AnnotationFinder annotationFinder = new AnnotationFinder( method );
                Annotation methodEndpointType = annotationFinder.findAnnotationAnnotatedDeeplyBy( MethodEndpointMarker.class );
                if( methodEndpointType != null )
                {
                    addChild( new ComponentMethodEndpointLifecycle( componentDescriptor, method, methodEndpointType ) );
                }
            }
            type = type.getSuperclass();
        }

        if( componentType.isAnnotationPresent( Service.class ) )
        {
            addChild( new ComponentServiceExporterLifecycle( this.componentDescriptor ) );
        }
    }

    @Override
    public final String toString()
    {
        return "ComponentSingleton[" + this.componentDescriptor + "]";
    }

    @Nullable
    Object getInstance()
    {
        return this.instance;
    }

    @Override
    protected synchronized void onBeforeActivate()
    {
        try
        {
            Constructor<?> defaultConstructor = this.componentDescriptor.getComponentType().getDeclaredConstructor();
            defaultConstructor.setAccessible( true );
            this.instance = defaultConstructor.newInstance();
        }
        catch( Exception e )
        {
            throw new ComponentCreateException( e, this.componentDescriptor.getComponentType(), this.componentDescriptor.getModule() );
        }
    }

    @Override
    protected synchronized void onAfterDeactivate()
    {
        if( this.instance != null )
        {
            for( Method preDestroyMethod : this.preDestroyMethods )
            {
                try
                {
                    preDestroyMethod.invoke( this.instance );
                }
                catch( Throwable e )
                {
                    LOG.warn( "The @PreDestroy method '{}' of component {} threw an exception: {}", preDestroyMethod, this, e.getMessage(), e );
                }
            }
            this.instance = null;
        }
    }
}
