package org.mosaic.validation.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.validation.*;
import javax.validation.bootstrap.ProviderSpecificBootstrap;
import javax.validation.executable.ExecutableValidator;
import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.HibernateValidatorConfiguration;
import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;
import org.hibernate.validator.spi.resourceloading.ResourceBundleLocator;
import org.mosaic.config.ResourceBundleManager;
import org.mosaic.lifecycle.Module;
import org.mosaic.lifecycle.ModuleManager;
import org.mosaic.lifecycle.annotation.Service;
import org.mosaic.lifecycle.annotation.ServiceRef;
import org.mosaic.util.reflection.MethodHandle;
import org.mosaic.util.weaving.MethodInterceptor;
import org.mosaic.validation.MethodValidationException;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;

import static java.util.Arrays.asList;

/**
 * @author arik
 */
@Service(MethodInterceptor.class)
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
                ProviderSpecificBootstrap<HibernateValidatorConfiguration> bootstrap = Validation.byProvider( HibernateValidator.class );
                HibernateValidatorConfiguration configuration = bootstrap.configure();

                MessageInterpolator messageInterpolator =
                        new ResourceBundleMessageInterpolator( new MosaicResourceBundleLocator( module ) );

                ParameterNameProvider parameterNameProvider =
                        new MosaicValidationParameterNameProvider( configuration.getDefaultParameterNameProvider() );

                ValidatorFactory validatorFactory = configuration
                        .constraintValidatorFactory( new MosaicConstraintValidatorFactory() )
                        .messageInterpolator( messageInterpolator )
                        .parameterNameProvider( parameterNameProvider )
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

    @Nullable
    @Override
    public Object intercept( @Nonnull MethodInvocation invocation ) throws Exception
    {
        MethodHandle methodHandle = invocation.getMethodHandle();
        if( !methodHandle.hasAnnotation( Valid.class ) )
        {
            return invocation.proceed();
        }

        Module module = this.moduleManager.getModuleFor( methodHandle.getDeclaringClass() );
        Object object = invocation.getObject();
        Object[] arguments = invocation.getArguments();

        Method method = methodHandle.getNativeMethod();
        ExecutableValidator execValidator = this.validators.get( module ).forExecutables();
        Set<ConstraintViolation<Object>> violations =
                execValidator.validateParameters( object, method, arguments );
        if( !violations.isEmpty() )
        {
            throw new MethodValidationException( methodHandle, getViolationMessages( violations ) );
        }

        Object result = invocation.proceed();
        violations = execValidator.validateReturnValue( object, method, result );
        if( !violations.isEmpty() )
        {
            throw new MethodValidationException( methodHandle, getViolationMessages( violations ) );
        }
        return result;
    }

    @Nonnull
    private Collection<String> getViolationMessages( @Nonnull Set<ConstraintViolation<Object>> violations )
    {
        List<String> messages = new LinkedList<>();
        for( ConstraintViolation<?> violation : violations )
        {
            messages.add( violation.getMessage() );
        }
        return messages;
    }

    private class MosaicConstraintValidatorFactory implements ConstraintValidatorFactory
    {
        @Override
        public <T extends ConstraintValidator<?, ?>> T getInstance( Class<T> key )
        {
            // TODO arik: implement getInstance([key])
            return null;
        }

        @Override
        public void releaseInstance( ConstraintValidator<?, ?> instance )
        {
            // TODO arik: implement releaseInstance([instance])
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
