package org.mosaic.web.handler.impl.parameters;

import com.google.common.reflect.TypeToken;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.util.reflection.MethodParameter;

/**
 * @author arik
 */
public abstract class CheckedParameterResolver<Type> extends AbstractWebParameterResolver
{
    @Nonnull
    private final TypeToken<? extends Type> checkedType;

    protected CheckedParameterResolver()
    {
        this.checkedType = new TypeToken<Type>( getClass() )
        {
        };
    }

    protected CheckedParameterResolver( @Nullable ConversionService conversionService )
    {
        super( conversionService );
        this.checkedType = new TypeToken<Type>( getClass() )
        {
        };
    }

    @Nullable
    @Override
    public final Object resolve( @Nonnull MethodParameter parameter, @Nonnull MapEx<String, Object> context )
            throws Exception
    {
        if( parameter.getType().isAssignableFrom( this.checkedType ) )
        {
            return resolveToType( parameter, context );
        }
        return SKIP;
    }

    @Nullable
    protected abstract Type resolveToType( @Nonnull MethodParameter parameter, @Nonnull MapEx<String, Object> context )
            throws Exception;
}
