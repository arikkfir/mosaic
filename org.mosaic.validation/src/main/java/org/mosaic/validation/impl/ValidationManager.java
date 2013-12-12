package org.mosaic.validation.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import javax.annotation.Nonnull;
import javax.validation.*;
import javax.validation.executable.ExecutableValidator;
import javax.validation.metadata.BeanDescriptor;
import javax.validation.spi.ValidationProvider;
import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.HibernateValidatorConfiguration;
import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;
import org.hibernate.validator.spi.resourceloading.ResourceBundleLocator;
import org.mosaic.event.EventListener;
import org.mosaic.modules.Component;
import org.mosaic.modules.ModuleEvent;
import org.mosaic.modules.ModuleEventType;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;

import static java.util.Arrays.asList;

/**
 * @author arik
 */
@Component
final class ValidationManager
{
    @Nonnull
    private final ThreadLocal<ClassLoader> classLoaderHolder = new ThreadLocal<>();

    @Nonnull
    private final Validator validator;

    ValidationManager()
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
                          .configure()
                          .ignoreXmlConfiguration()
                          .messageInterpolator( new ResourceBundleMessageInterpolator( new MosaicAwareResourceBundleLocator(), false ) )
                          .parameterNameProvider( new MosaicValidationParameterNameProvider() );
        this.validator = new MosaicAwareValidator( configuration.buildValidatorFactory().getValidator() );
    }

    @Nonnull
    Validator getValidator()
    {
        return this.validator;
    }

    @EventListener
    void moduleResolvedOrUnresolved( @Nonnull ModuleEvent event )
    {
        if( event.getEventType() == ModuleEventType.UNRESOLVED )
        {
            ResourceBundle.clearCache();
        }
    }

    private class MosaicAwareValidator implements Validator, ExecutableValidator
    {
        @Nonnull
        private final Validator validator;

        @Nonnull
        private final ExecutableValidator executableValidator;

        private MosaicAwareValidator( @Nonnull Validator validator )
        {
            this.validator = validator;
            this.executableValidator = this.validator.forExecutables();
        }

        @Override
        public <T> Set<ConstraintViolation<T>> validate( T object, Class<?>... groups )
        {
            ClassLoader prev = classLoaderHolder.get();
            try
            {
                classLoaderHolder.set( object.getClass().getClassLoader() );
                return this.validator.validate( object, groups );
            }
            finally
            {
                classLoaderHolder.set( prev );
            }
        }

        @Override
        public <T> Set<ConstraintViolation<T>> validateProperty( T object, String propertyName, Class<?>... groups )
        {
            ClassLoader prev = classLoaderHolder.get();
            try
            {
                classLoaderHolder.set( object.getClass().getClassLoader() );
                return this.validator.validateProperty( object, propertyName, groups );
            }
            finally
            {
                classLoaderHolder.set( prev );
            }
        }

        @Override
        public <T> Set<ConstraintViolation<T>> validateValue( Class<T> beanType,
                                                              String propertyName,
                                                              Object value,
                                                              Class<?>... groups )
        {
            ClassLoader prev = classLoaderHolder.get();
            try
            {
                classLoaderHolder.set( beanType.getClassLoader() );
                return this.validator.validateValue( beanType, propertyName, value, groups );
            }
            finally
            {
                classLoaderHolder.set( prev );
            }
        }

        @Override
        public BeanDescriptor getConstraintsForClass( Class<?> clazz )
        {
            ClassLoader prev = classLoaderHolder.get();
            try
            {
                classLoaderHolder.set( clazz.getClassLoader() );
                return this.validator.getConstraintsForClass( clazz );
            }
            finally
            {
                classLoaderHolder.set( prev );
            }
        }

        @Override
        public <T> T unwrap( Class<T> type )
        {
            throw new ValidationException( "Type " + type.getName() + " not supported for unwrapping." );
        }

        @Override
        public ExecutableValidator forExecutables()
        {
            return this;
        }

        @Override
        public <T> Set<ConstraintViolation<T>> validateParameters( T object,
                                                                   Method method,
                                                                   Object[] parameterValues,
                                                                   Class<?>... groups )
        {
            ClassLoader prev = classLoaderHolder.get();
            try
            {
                classLoaderHolder.set( object != null
                                       ? object.getClass().getClassLoader()
                                       : method.getDeclaringClass().getClassLoader() );
                return this.executableValidator.validateParameters( object, method, parameterValues, groups );
            }
            finally
            {
                classLoaderHolder.set( prev );
            }
        }

        @Override
        public <T> Set<ConstraintViolation<T>> validateReturnValue( T object,
                                                                    Method method,
                                                                    Object returnValue,
                                                                    Class<?>... groups )
        {
            ClassLoader prev = classLoaderHolder.get();
            try
            {
                classLoaderHolder.set( object != null
                                       ? object.getClass().getClassLoader()
                                       : method.getDeclaringClass().getClassLoader() );
                return this.executableValidator.validateReturnValue( object, method, returnValue, groups );
            }
            finally
            {
                classLoaderHolder.set( prev );
            }
        }

        @Override
        public <T> Set<ConstraintViolation<T>> validateConstructorParameters( Constructor<? extends T> constructor,
                                                                              Object[] parameterValues,
                                                                              Class<?>... groups )
        {
            ClassLoader prev = classLoaderHolder.get();
            try
            {
                classLoaderHolder.set( constructor.getDeclaringClass().getClassLoader() );
                return this.executableValidator.validateConstructorParameters( constructor, parameterValues, groups );
            }
            finally
            {
                classLoaderHolder.set( prev );
            }
        }

        @Override
        public <T> Set<ConstraintViolation<T>> validateConstructorReturnValue( Constructor<? extends T> constructor,
                                                                               T createdObject,
                                                                               Class<?>... groups )
        {
            ClassLoader prev = classLoaderHolder.get();
            try
            {
                classLoaderHolder.set( createdObject.getClass().getClassLoader() );
                return this.executableValidator.validateConstructorReturnValue( constructor, createdObject, groups );
            }
            finally
            {
                classLoaderHolder.set( prev );
            }
        }
    }

    private class MosaicAwareResourceBundleLocator implements ResourceBundleLocator
    {
        @Override
        public ResourceBundle getResourceBundle( Locale locale )
        {
            return ResourceBundle.getBundle( "ValidationMessages", locale, classLoaderHolder.get() );
        }
    }

    private class MosaicValidationParameterNameProvider implements ParameterNameProvider
    {
        @Nonnull
        private final ParameterNameDiscoverer parameterNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();

        @Override
        public List<String> getParameterNames( Constructor<?> constructor )
        {
            return asList( this.parameterNameDiscoverer.getParameterNames( constructor ) );
        }

        @Override
        public List<String> getParameterNames( Method method )
        {
            return asList( this.parameterNameDiscoverer.getParameterNames( method ) );
        }
    }
}
