package org.mosaic.modules.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.modules.*;
import org.mosaic.util.osgi.FilterBuilder;
import org.mosaic.util.pair.Pair;
import org.mosaic.util.reflection.AnnotationFinder;

import static java.lang.reflect.Modifier.isFinal;
import static java.lang.reflect.Modifier.isStatic;

/**
 * @author arik
 */
@SuppressWarnings("unchecked")
final class ComponentDescriptorImpl<Type> extends Lifecycle implements ComponentDescriptor<Type>
{
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

    @Nonnull
    private final ModuleImpl module;

    @Nonnull
    private final Class<Type> componentType;

    private final boolean plain;

    private final boolean singleton;

    @Nonnull
    private final Map<String, ComponentField> fieldDescriptors = new ConcurrentHashMap<>();

    ComponentDescriptorImpl( @Nonnull ModuleImpl module, @Nonnull Class<Type> componentType )
    {
        this.module = module;
        this.componentType = componentType;

        // fields should be the first children
        for( Field field : this.componentType.getDeclaredFields() )
        {
            int modifiers = field.getModifiers();
            if( !isStatic( modifiers ) && !isFinal( modifiers ) )
            {
                if( field.isAnnotationPresent( Component.class ) )
                {
                    this.fieldDescriptors.put( field.getName(), new ComponentFieldComponentLifecycle( this, field ) );
                }
                else if( field.isAnnotationPresent( Service.class ) )
                {
                    if( field.getType().isAssignableFrom( ServiceReference.class ) )
                    {
                        this.fieldDescriptors.put( field.getName(), new ComponentFieldServiceReferenceLifecycle( this, field ) );
                    }
                    else if( field.getType().isAssignableFrom( List.class ) )
                    {
                        this.fieldDescriptors.put( field.getName(), new ComponentFieldServiceListLifecycle( this, field ) );
                    }
                    else
                    {
                        this.fieldDescriptors.put( field.getName(), new ComponentFieldServiceProxyLifecycle( this, field ) );
                    }
                }
            }
        }
        for( ComponentField fieldLifecycle : this.fieldDescriptors.values() )
        {
            addChild( fieldLifecycle );
        }

        // now detect singletons, adapters, etc
        boolean canBeComponent = !this.componentType.isAnnotation()
                                 && !this.componentType.isAnonymousClass()
                                 && !this.componentType.isArray()
                                 && !this.componentType.isEnum()
                                 && !this.componentType.isInterface()
                                 && !this.componentType.isLocalClass()
                                 && !this.componentType.isMemberClass()
                                 && !this.componentType.isPrimitive()
                                 && !this.componentType.isSynthetic();

        this.singleton = this.componentType.isAnnotationPresent( Service.class ) || this.componentType.isAnnotationPresent( Component.class );
        if( this.singleton )
        {
            if( !canBeComponent )
            {
                throw new ComponentDefinitionException( "cannot use @Service on non-concrete classes", this.componentType, this.module );
            }
            else
            {
                addChild( new ComponentSingletonLifecycle( this, this.componentType ) );
            }
        }
        else if( this.componentType.isAnnotationPresent( Adapter.class ) )
        {
            if( !canBeComponent )
            {
                throw new ComponentDefinitionException( "cannot use @Adapter on non-concrete classes", this.componentType, this.module );
            }
            else
            {
                addChild( new ComponentServiceAdapterLifecycle( this ) );
            }
        }
        else if( this.componentType.isInterface() )
        {
            AnnotationFinder annotationFinder = new AnnotationFinder( this.componentType );
            Annotation templateType = annotationFinder.findAnnotationAnnotatedDeeplyBy( Template.class );
            if( templateType != null )
            {
                addChild( new ComponentServiceTemplateExporterLifecycle( this, templateType ) );
            }
        }
        this.plain = this.fieldDescriptors.isEmpty()
                     && getChild( ComponentSingletonLifecycle.class ) == null
                     && getChild( ComponentServiceAdapterLifecycle.class ) == null
                     && getChild( ComponentServiceTemplateExporterLifecycle.class ) == null;
    }

    public boolean isPlain()
    {
        return this.plain;
    }

    @Override
    public String toString()
    {
        return "Component[" + this.componentType.getName() + " in " + this.module + "]";
    }

    @Nullable
    @Override
    public Object getValueForField( @Nonnull String fieldName )
    {
        ComponentField field = this.fieldDescriptors.get( fieldName );
        if( field == null )
        {
            throw new IllegalArgumentException( "unknown field '" + fieldName + "' for component type '" + this.componentType.getName() + "'" );
        }
        else
        {
            return field.getValue();
        }
    }

    @Nonnull
    @Override
    public Type getInstance()
    {
        if( !isActivated() )
        {
            throw new IllegalStateException( "module " + this.module + " is not active" );
        }
        else if( this.singleton )
        {
            Object instance = requireChild( ComponentSingletonLifecycle.class ).getInstance();
            if( instance == null )
            {
                throw new IllegalStateException( "singleton instance of '" + this.componentType.getName() + "' has not been created yet" );
            }
            else
            {
                return this.componentType.cast( instance );
            }
        }
        else
        {
            try
            {
                return this.componentType.newInstance();
            }
            catch( Throwable e )
            {
                throw new ComponentCreateException( e, this.componentType, this.module );
            }
        }
    }

    @Nonnull
    Class<Type> getComponentType()
    {
        return componentType;
    }

    @Nonnull
    ModuleImpl getModule()
    {
        return this.module;
    }
}
