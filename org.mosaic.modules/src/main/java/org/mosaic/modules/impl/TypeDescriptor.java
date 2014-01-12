package org.mosaic.modules.impl;

import com.google.common.collect.Sets;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.modules.*;
import org.mosaic.util.reflection.AnnotationFinder;

import static java.lang.reflect.Modifier.isFinal;
import static java.lang.reflect.Modifier.isStatic;

/**
 * @author arik
 */
@SuppressWarnings("unchecked")
final class TypeDescriptor extends Lifecycle implements org.mosaic.modules.TypeDescriptor
{
    private static final Set<Class<?>> MODULE_TYPES = Sets.newHashSet( Module.class,
                                                                       ModuleTypes.class,
                                                                       ModuleResources.class,
                                                                       ModuleWiring.class );

    @Nonnull
    private final ModuleImpl module;

    @Nonnull
    private final Class<?> type;

    @Nonnull
    private final Map<String, TypeDescriptorField> fields;

    TypeDescriptor( @Nonnull ModuleImpl module, @Nonnull Class<?> type )
    {
        this.module = module;
        this.type = type;

        AnnotationFinder annotationFinder = new AnnotationFinder( this.type );

        // is this is a @Component?
        if( annotationFinder.findDeep( org.mosaic.modules.Component.class ) != null )
        {
            addChild( new Component( this ) );
        }

        // is this a @Adapter?
        if( annotationFinder.findDeep( Adapter.class ) != null )
        {
            addChild( new TypeDescriptorServiceAdapter( this ) );
        }

        // is this a @Template?
        if( this.type.isInterface() )
        {
            Annotation templateType = annotationFinder.findAnnotationAnnotatedDeeplyBy( Template.class );
            if( templateType != null )
            {
                addChild( new TypeDescriptorServiceTemplateExporter( this, templateType ) );
            }
        }

        // discover managed fields
        Map<String, TypeDescriptorField> fields = new ConcurrentHashMap<>( 10 );
        for( Field field : this.type.getDeclaredFields() )
        {
            int modifiers = field.getModifiers();
            if( !isStatic( modifiers ) && !isFinal( modifiers ) )
            {
                if( field.isAnnotationPresent( org.mosaic.modules.Component.class ) )
                {
                    if( MODULE_TYPES.contains( field.getType() ) )
                    {
                        fields.put( field.getName(), new TypeDescriptorFieldModule( this, field ) );
                    }
                    else if( field.getType().isAssignableFrom( List.class ) )
                    {
                        fields.put( field.getName(), new TypeDescriptorFieldComponentList( this, field ) );
                    }
                    else
                    {
                        fields.put( field.getName(), new TypeDescriptorFieldComponent( this, field ) );
                    }
                }
                else if( field.isAnnotationPresent( Service.class ) )
                {
                    if( field.getType().isAssignableFrom( ServiceReference.class ) )
                    {
                        fields.put( field.getName(), new TypeDescriptorFieldServiceReference( this, field ) );
                    }
                    else if( field.getType().isAssignableFrom( List.class ) )
                    {
                        fields.put( field.getName(), new TypeDescriptorFieldServiceList( this, field ) );
                    }
                    else
                    {
                        fields.put( field.getName(), new TypeDescriptorFieldServiceProxy( this, field ) );
                    }
                }
            }
        }
        this.fields = fields;

        // add all fields to the lifecycle
        for( TypeDescriptorField fieldLifecycle : this.fields.values() )
        {
            addChild( fieldLifecycle );
        }
    }

    @Override
    public String toString()
    {
        return "Type[" + this.type + " / " + this.module + "]";
    }

    @Nullable
    @Override
    public Object getValueForField( @Nonnull String fieldName )
    {
        TypeDescriptorField field = this.fields.get( fieldName );
        if( field == null )
        {
            throw new IllegalArgumentException( "unknown field '" + fieldName + "' for component type '" + this.type + "'" );
        }
        else
        {
            return field.getValue();
        }
    }

    @Nonnull
    ModuleImpl getModule()
    {
        return this.module;
    }

    @Nonnull
    Class<?> getType()
    {
        return this.type;
    }
}
