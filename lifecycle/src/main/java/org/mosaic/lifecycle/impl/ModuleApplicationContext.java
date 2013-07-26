package org.mosaic.lifecycle.impl;

import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.mosaic.lifecycle.Module;
import org.mosaic.lifecycle.ModuleBeanNameNotFoundException;
import org.mosaic.lifecycle.ModuleBeanTypeNotFoundException;
import org.mosaic.lifecycle.ModuleState;
import org.mosaic.lifecycle.annotation.BeanRef;
import org.osgi.framework.BundleContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;

import static java.util.Arrays.asList;

/**
 * @author arik
 */
public class ModuleApplicationContext extends StaticApplicationContext
{
    @Nonnull
    private final ModuleImpl module;

    public ModuleApplicationContext( @Nonnull ModuleImpl module, Set<Class<?>> componentClasses )
    {
        this.module = module;

        ClassLoader classLoader = this.module.getClassLoader();
        if( classLoader == null )
        {
            throw new IllegalStateException( "No ClassLoader is available for module '" + module + "'" );
        }

        setId( this.module.toString() );
        setDisplayName( this.module.getName() + "-" + this.module.getVersion() );
        setAllowBeanDefinitionOverriding( false );
        setAllowCircularReferences( false );
        setClassLoader( classLoader );
        setEnvironment( new ModuleEnvironment() );
        setResourceLoader( new DefaultResourceLoader( classLoader ) );

        // add bean autowiring post processor
        AutowiredAnnotationBeanPostProcessor autowiredAnnotationBeanPostProcessor = new AutowiredAnnotationBeanPostProcessor();
        autowiredAnnotationBeanPostProcessor.setAutowiredAnnotationTypes( new HashSet<>( asList( BeanRef.class, Autowired.class ) ) );
        autowiredAnnotationBeanPostProcessor.setBeanFactory( getBeanFactory() );
        autowiredAnnotationBeanPostProcessor.setRequiredParameterName( "required" );
        autowiredAnnotationBeanPostProcessor.setRequiredParameterValue( true );
        getBeanFactory().addBeanPostProcessor( autowiredAnnotationBeanPostProcessor );

        // add bean lifecycle post processor: ensures we inject module dependencies
        getBeanFactory().addBeanPostProcessor( new BeanLifecyclePostProcessor() );

        // add bean construction/destruction post processor
        InitDestroyAnnotationBeanPostProcessor initDestroyAnnotationBeanPostProcessor = new InitDestroyAnnotationBeanPostProcessor();
        initDestroyAnnotationBeanPostProcessor.setDestroyAnnotationType( PreDestroy.class );
        initDestroyAnnotationBeanPostProcessor.setInitAnnotationType( PostConstruct.class );
        getBeanFactory().addBeanPostProcessor( initDestroyAnnotationBeanPostProcessor );

        // register the beans in the application context
        for( Class<?> componentClass : componentClasses )
        {
            String beanName = componentClass.getName();
            BeanDefinition beanDef = new AnnotatedGenericBeanDefinition( componentClass );
            registerBeanDefinition( beanName, beanDef );
        }
    }

    public <T> T findModuleBean( @Nonnull Class<? extends T> type )
    {
        if( this.module.getState() != ModuleState.ACTIVE )
        {
            throw new IllegalStateException( "Module '" + this + "' is not active" );
        }
        else
        {
            try
            {
                return getBean( type );
            }
            catch( BeansException e )
            {
                throw new ModuleBeanTypeNotFoundException( e, this.module, type );
            }
        }
    }

    public <T> T findModuleBean( @Nonnull String beanName, @Nonnull Class<? extends T> type )
    {
        if( this.module.getState() != ModuleState.ACTIVE )
        {
            throw new IllegalStateException( "Module '" + this + "' is not active" );
        }
        else
        {
            try
            {
                return getBean( beanName, type );
            }
            catch( BeansException e )
            {
                throw new ModuleBeanNameNotFoundException( e, this.module, beanName, type );
            }
        }
    }

    private class ModulePropertySource extends PropertySource<Module>
    {
        private ModulePropertySource()
        {
            super( module.getName() + "-" + module.getVersion(), module );
        }

        @Override
        public Object getProperty( String name )
        {
            BundleContext bundleContext = module.getBundle().getBundleContext();
            return bundleContext == null ? null : bundleContext.getProperty( name );
        }
    }

    private class ModuleEnvironment extends StandardEnvironment
    {
        @Override
        protected void customizePropertySources( MutablePropertySources propertySources )
        {
            super.customizePropertySources( propertySources );
            propertySources.addFirst( new ModulePropertySource() );
        }
    }

    private class BeanLifecyclePostProcessor implements BeanPostProcessor
    {
        @Override
        public Object postProcessBeforeInitialization( Object bean, String beanName ) throws BeansException
        {
            module.beanCreated( bean, beanName );
            return bean;
        }

        @Override
        public Object postProcessAfterInitialization( Object bean, String beanName ) throws BeansException
        {
            module.beanInitialized( bean, beanName );
            return bean;
        }
    }
}
