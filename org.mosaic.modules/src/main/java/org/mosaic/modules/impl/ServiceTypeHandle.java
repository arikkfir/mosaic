package org.mosaic.modules.impl;

import com.google.common.reflect.TypeToken;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import javax.annotation.Nonnull;
import org.mosaic.modules.MethodEndpoint;
import org.mosaic.modules.ServiceReference;
import org.mosaic.modules.ServiceTemplate;
import org.mosaic.util.osgi.FilterBuilder;
import org.mosaic.util.reflection.TypeTokens;

import static java.util.Arrays.asList;

/**
 * @author arik
 */
final class ServiceTypeHandle
{
    private static final Class[] ALL_ALLOWED = new Class[] { Token.class };

    @Nonnull
    static Token<?> createToken( @Nonnull Type type, @Nonnull Class<?>... allowed )
    {
        if( allowed.length == 0 )
        {
            allowed = ALL_ALLOWED;
        }
        Token<?> token;

        if( type instanceof ParameterizedType )
        {
            ParameterizedType parameterizedType = ( ParameterizedType ) type;
            Type rawType = parameterizedType.getRawType();
            if( List.class.equals( rawType ) )
            {
                token = new ServiceListToken( parameterizedType );
            }
            else if( ServiceReference.class.equals( rawType ) )
            {
                token = new ServiceReferenceToken( parameterizedType );
            }
            else if( MethodEndpoint.class.equals( rawType ) )
            {
                token = new MethodEndpointServiceToken( parameterizedType );
            }
            else if( ServiceTemplate.class.equals( rawType ) )
            {
                token = new ServiceTemplateServiceToken( parameterizedType );
            }
            else
            {
                throw new IllegalArgumentException( "Cannot create token for '" + type + "'" );
            }
        }
        else
        {
            token = new ServiceToken( type );
        }

        for( Class<?> allowedType : allowed )
        {
            if( allowedType.isInstance( token ) )
            {
                return token;
            }
        }
        throw new IllegalArgumentException( "Token '" + token + "' for type '" + type + "' was not allowed - only token types " + asList( allowed ) + " are allowed" );
    }

    static abstract class Token<T extends Type>
    {
        @Nonnull
        protected final T type;

        @Nonnull
        protected final TypeToken<?> typeToken;

        protected Token( @Nonnull T type )
        {
            this.type = type;
            this.typeToken = TypeTokens.of( this.type );
        }

        @Nonnull
        Class<?> getServiceClass()
        {
            return this.typeToken.getRawType();
        }

        @Nonnull
        FilterBuilder createFilterBuilder()
        {
            return new FilterBuilder().addClass( getServiceClass() );
        }

        @Override
        public String toString()
        {
            return getClass().getSimpleName() + "[" + this.type + "]";
        }
    }

    static abstract class ParameterizedToken extends Token<ParameterizedType>
    {
        protected ParameterizedToken( @Nonnull ParameterizedType type )
        {
            super( type );

            java.lang.reflect.Type[] typeArgs = this.type.getActualTypeArguments();
            if( typeArgs.length != 1 )
            {
                throw new IllegalArgumentException( getClass().getSimpleName() + " tokens must be parameterized" );
            }
        }
    }

    static class ServiceToken extends Token<Type>
    {
        ServiceToken( @Nonnull Type type )
        {
            super( type );
        }
    }

    static final class MethodEndpointServiceToken extends ParameterizedToken
    {
        @Nonnull
        private final Class<? extends Annotation> methodEndpointType;

        MethodEndpointServiceToken( @Nonnull ParameterizedType type )
        {
            super( type );

            Type typeArg = this.type.getActualTypeArguments()[ 0 ];
            if( !Class.class.isInstance( typeArg ) )
            {
                throw new IllegalArgumentException( getClass().getSimpleName() + " must have annotation type parameter" );
            }

            this.methodEndpointType = ( ( Class<?> ) typeArg ).asSubclass( Annotation.class );
        }

        @Nonnull
        @Override
        FilterBuilder createFilterBuilder()
        {
            return super.createFilterBuilder().addEquals( "type", this.methodEndpointType.getName() );
        }
    }

    static final class ServiceTemplateServiceToken extends ParameterizedToken
    {
        @Nonnull
        private final Class<? extends Annotation> templateType;

        ServiceTemplateServiceToken( @Nonnull ParameterizedType type )
        {
            super( type );

            Type typeArg = this.type.getActualTypeArguments()[ 0 ];
            if( !Class.class.isInstance( typeArg ) )
            {
                throw new IllegalArgumentException( getClass().getSimpleName() + " must have annotation type parameter" );
            }

            this.templateType = ( ( Class<?> ) typeArg ).asSubclass( Annotation.class );
        }

        @Nonnull
        @Override
        FilterBuilder createFilterBuilder()
        {
            return super.createFilterBuilder().addEquals( "type", this.templateType.getName() );
        }
    }

    static final class ServiceReferenceToken extends ParameterizedToken
    {
        @Nonnull
        protected final Token<?> target;

        ServiceReferenceToken( @Nonnull ParameterizedType type )
        {
            super( type );
            this.target = createToken( this.type.getActualTypeArguments()[ 0 ],
                                       ServiceToken.class,
                                       MethodEndpointServiceToken.class,
                                       ServiceTemplateServiceToken.class );
        }

        @Nonnull
        public Token<?> getTarget()
        {
            return this.target;
        }

        @Nonnull
        @Override
        Class<?> getServiceClass()
        {
            return this.target.getServiceClass();
        }

        @Nonnull
        @Override
        FilterBuilder createFilterBuilder()
        {
            return this.target.createFilterBuilder();
        }
    }

    static final class ServiceListToken extends ParameterizedToken
    {
        @Nonnull
        private final Token<?> itemType;

        ServiceListToken( @Nonnull ParameterizedType type )
        {
            super( type );
            this.itemType = createToken( this.type.getActualTypeArguments()[ 0 ],
                                         ServiceToken.class,
                                         MethodEndpointServiceToken.class,
                                         ServiceTemplateServiceToken.class,
                                         ServiceReferenceToken.class );
        }

        @Nonnull
        public Token<?> getItemType()
        {
            return this.itemType;
        }

        @Nonnull
        @Override
        Class<?> getServiceClass()
        {
            return this.itemType.getServiceClass();
        }

        @Nonnull
        @Override
        FilterBuilder createFilterBuilder()
        {
            return this.itemType.createFilterBuilder();
        }
    }
}
