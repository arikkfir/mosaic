package org.mosaic.web.handler.impl.parameters;

import com.google.common.reflect.TypeToken;
import java.lang.annotation.Annotation;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.util.reflection.MethodParameter;

/**
 * @author arik
 */
public abstract class AnnotatedParameterResolver<AnnType extends Annotation> extends AbstractWebParameterResolver
{
    @Nonnull
    private final Class<AnnType> annotationType;

    @SuppressWarnings("unchecked")
    protected AnnotatedParameterResolver( @Nullable ConversionService conversionService )
    {
        super( conversionService );
        TypeToken<? extends AnnType> typeToken = new TypeToken<AnnType>( getClass() )
        {
        };
        this.annotationType = ( Class<AnnType> ) typeToken.getRawType();
    }

    @Nullable
    @Override
    public final Object resolve( @Nonnull MethodParameter parameter, @Nonnull MapEx<String, Object> context )
            throws Exception
    {
        AnnType annotation = parameter.getAnnotation( this.annotationType );
        if( annotation != null )
        {
            return resolveWithAnnotation( parameter, context, annotation );
        }
        return SKIP;
    }

    @Nullable
    protected abstract Object resolveWithAnnotation( @Nonnull MethodParameter parameter,
                                                     @Nonnull MapEx<String, Object> context,
                                                     @Nonnull AnnType annotation ) throws Exception;
}
