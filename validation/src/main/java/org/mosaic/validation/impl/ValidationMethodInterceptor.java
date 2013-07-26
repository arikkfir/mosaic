package org.mosaic.validation.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.validation.*;
import javax.validation.executable.ExecutableValidator;
import javax.validation.spi.ValidationProvider;
import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.HibernateValidatorConfiguration;
import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;
import org.hibernate.validator.spi.resourceloading.ResourceBundleLocator;
import org.mosaic.config.ResourceBundleManager;
import org.mosaic.lifecycle.Module;
import org.mosaic.lifecycle.ModuleBeanNotFoundException;
import org.mosaic.lifecycle.ModuleManager;
import org.mosaic.lifecycle.annotation.Service;
import org.mosaic.lifecycle.annotation.ServiceRef;
import org.mosaic.util.reflection.MethodHandle;
import org.mosaic.util.reflection.MethodParameter;
import org.mosaic.util.weaving.MethodInterceptor;
import org.mosaic.validation.MethodValidationException;
import org.mosaic.web.handler.annotation.ExceptionHandler;
import org.mosaic.web.net.HttpStatus;
import org.mosaic.web.request.WebRequest;
import org.mosaic.web.request.WebResponse;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;

import static java.util.Arrays.asList;

/**
 * @author arik
 */
@Service( MethodInterceptor.class )
public class ValidationMethodInterceptor implements MethodInterceptor
{
    @Nonnull
    private final LoadingCache<Module, Validator> validators;

    @Nonnull
    private ModuleManager moduleManager;

    @Nonnull
    private ResourceBundleManager resourceBundleManager;

    public ValidationMethodInterceptor()
    {
        CacheLoader<Module, Validator> validatorCacheLoader = new CacheLoader<Module, Validator>()
        {
            @Override
            public Validator load( Module module ) throws Exception
            {
                HibernateValidatorConfiguration configuration =
                        Validation.byProvider( HibernateValidator.class )
                                  .providerResolver( new ValidationProviderResolver()
                                  {
                                      @Override
                                      public List<ValidationProvider<?>> getValidationProviders()
                                      {
                                          return Arrays.<ValidationProvider<?>>asList( new HibernateValidator() );
                                      }
                                  } )
                                  .configure();

                ValidatorFactory validatorFactory = configuration
                        .constraintValidatorFactory( new MosaicConstraintValidatorFactory( configuration.getDefaultConstraintValidatorFactory() ) )
                        .messageInterpolator( new ResourceBundleMessageInterpolator( new MosaicResourceBundleLocator( module ) ) )
                        .parameterNameProvider( new MosaicValidationParameterNameProvider( configuration.getDefaultParameterNameProvider() ) )
                        .ignoreXmlConfiguration()
                        .buildValidatorFactory();

                return validatorFactory.getValidator();
            }
        };

        this.validators = CacheBuilder.newBuilder()
                                      .concurrencyLevel( 10 )
                                      .weakKeys()
                                      .maximumSize( 5000 )
                                      .expireAfterAccess( 1, TimeUnit.HOURS )
                                      .build( validatorCacheLoader );
    }

    @ServiceRef
    public void setResourceBundleManager( @Nonnull ResourceBundleManager resourceBundleManager )
    {
        this.resourceBundleManager = resourceBundleManager;
    }

    @ServiceRef
    public void setModuleManager( @Nonnull ModuleManager moduleManager )
    {
        this.moduleManager = moduleManager;
    }

    @ExceptionHandler
    public void handleWebHandlersValidationErrors( @Nonnull WebRequest request,
                                                   @Nonnull MethodValidationException exception )
    {
        WebResponse response = request.getResponse();
        response.setStatus( HttpStatus.BAD_REQUEST );
        response.disableCaching();

        for( ConstraintViolation<?> violation : exception.getViolations() )
        {
            response.getHeaders().addHeader( "X-Mosaic-Validation-Violation",
                                             violation.getPropertyPath() + ": " + violation.getMessage() );
        }
    }

    @SuppressWarnings( "unchecked" )
    @Nullable
    @Override
    public Object intercept( @Nonnull MethodInvocation invocation ) throws Exception
    {
        MethodHandle methodHandle = invocation.getMethodHandle();
        if( !methodHandle.hasAnnotation( Valid.class ) )
        {
            boolean found = false;
            for( MethodParameter parameter : methodHandle.getParameters() )
            {
                if( parameter.hasAnnotation( Valid.class ) )
                {
                    found = true;
                    break;
                }
                else
                {
                    for( Annotation annotation : parameter.getAnnotations() )
                    {
                        if( annotation.annotationType().isAnnotationPresent( Constraint.class ) )
                        {
                            found = true;
                            break;
                        }
                    }
                    if( found )
                    {
                        break;
                    }
                }
            }
            if( !found )
            {
                return invocation.proceed();
            }
        }

        Module module = this.moduleManager.getModuleFor( methodHandle.getDeclaringClass() );
        Object object = invocation.getObject();
        Object[] arguments = invocation.getArguments();

        Method method = methodHandle.getNativeMethod();
        ExecutableValidator execValidator = this.validators.get( module ).forExecutables();
        Set violations =
                execValidator.validateParameters( object, method, arguments );
        if( !violations.isEmpty() )
        {
            throw new MethodValidationException( methodHandle, violations );
        }

        Object result = invocation.proceed();
        violations = execValidator.validateReturnValue( object, method, result );
        if( !violations.isEmpty() )
        {
            throw new MethodValidationException( methodHandle, violations );
        }
        return result;
    }

    private class MosaicConstraintValidatorFactory implements ConstraintValidatorFactory
    {
        @Nonnull
        private final ConstraintValidatorFactory defaultConstraintValidatorFactory;

        public MosaicConstraintValidatorFactory( @Nonnull ConstraintValidatorFactory defaultConstraintValidatorFactory )
        {
            this.defaultConstraintValidatorFactory = defaultConstraintValidatorFactory;
        }

        @Override
        public <T extends ConstraintValidator<?, ?>> T getInstance( Class<T> key )
        {
            Module module = moduleManager.getModuleFor( key );
            if( module != null )
            {
                try
                {
                    return module.getBean( key );
                }
                catch( ModuleBeanNotFoundException ignore )
                {
                }
            }
            return this.defaultConstraintValidatorFactory.getInstance( key );
        }

        @Override
        public void releaseInstance( ConstraintValidator<?, ?> instance )
        {
            // no-op
        }
    }

    private class MosaicResourceBundleLocator implements ResourceBundleLocator
    {
        @Nonnull
        private final Module module;

        private MosaicResourceBundleLocator( @Nonnull Module module )
        {
            this.module = module;
        }

        @Override
        public ResourceBundle getResourceBundle( Locale locale )
        {
            return resourceBundleManager.getResourceBundle( this.module.getName(), locale );
        }
    }

    private class MosaicValidationParameterNameProvider implements ParameterNameProvider
    {
        @Nonnull
        private final ParameterNameProvider defaultParameterNameProvider;

        @Nonnull
        private final LocalVariableTableParameterNameDiscoverer parameterNameDiscoverer;

        public MosaicValidationParameterNameProvider( @Nonnull ParameterNameProvider defaultParameterNameProvider )
        {
            this.defaultParameterNameProvider = defaultParameterNameProvider;
            this.parameterNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();
        }

        @Override
        public List<String> getParameterNames( Constructor<?> constructor )
        {
            String[] parameterNames = this.parameterNameDiscoverer.getParameterNames( constructor );
            if( parameterNames == null )
            {
                return this.defaultParameterNameProvider.getParameterNames( constructor );
            }
            else
            {
                return asList( parameterNames );
            }
        }

        @Override
        public List<String> getParameterNames( Method method )
        {
            String[] parameterNames = this.parameterNameDiscoverer.getParameterNames( method );
            if( parameterNames == null )
            {
                return this.defaultParameterNameProvider.getParameterNames( method );
            }
            else
            {
                return asList( parameterNames );
            }
        }
    }
}
