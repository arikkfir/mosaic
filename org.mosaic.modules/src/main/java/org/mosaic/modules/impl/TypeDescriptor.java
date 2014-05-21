package org.mosaic.modules.impl;

import com.google.common.collect.Sets;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.modules.*;

import static java.lang.reflect.Modifier.isFinal;
import static java.lang.reflect.Modifier.isStatic;
import static org.mosaic.util.reflection.ClassAnnotations.getMetaAnnotation;
import static org.mosaic.util.reflection.ClassAnnotations.getMetaAnnotationTarget;

/**
 * @author arik
 */
@SuppressWarnings("unchecked")
final class TypeDescriptor extends Lifecycle implements org.mosaic.modules.TypeDescriptor
{
    private static final Set<Class<?>> MODULE_TYPES = Sets.<Class<?>>newHashSet( Module.class );

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

        // is this is a @Component?
        boolean isAbstract = Modifier.isAbstract( type.getModifiers() );
        boolean isInterface = type.isInterface();
        boolean isAnnotation = type.isAnnotation();
        boolean isAnonymousClass = type.isAnonymousClass();
        boolean isEnum = type.isEnum();
        boolean isMemberClass = type.isMemberClass();
        boolean isLocalClass = type.isLocalClass();
        if( !isAbstract && !isInterface && !isAnnotation && !isAnonymousClass && !isEnum && !isMemberClass && !isLocalClass )
        {
            // is this a @Component?
            if( getMetaAnnotation( type, org.mosaic.modules.Component.class ) != null )
            {
                addChild( new Component( this ) );
            }

            // is this a @Adapter?
            if( getMetaAnnotation( type, Adapter.class ) != null )
            {
                addChild( new TypeDescriptorServiceAdapter( this ) );
            }
        }

        // is this an interface that has a @Template-annotated annotation? (eg. has a @Dao annotation which has @Template)
        if( this.type.isInterface() )
        {
            Annotation templateType = getMetaAnnotationTarget( this.type, Template.class );
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
    @Override
    public ModuleImpl getModule()
    {
        return this.module;
    }

    @Override
    @Nonnull
    public Class<?> getType()
    {
        return this.type;
    }
}
