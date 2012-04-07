package org.mosaic.server.boot.impl.publish;

import java.util.Collection;
import java.util.HashSet;
import org.mosaic.logging.Logger;
import org.mosaic.logging.LoggerFactory;
import org.mosaic.osgi.util.BundleUtils;
import org.mosaic.server.boot.impl.publish.requirement.Requirement;
import org.mosaic.server.boot.impl.publish.requirement.RequirementFactory;
import org.mosaic.server.boot.impl.publish.spring.BundleApplicationContext;
import org.mosaic.server.boot.impl.publish.spring.OsgiSpringNamespacePlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import static org.mosaic.server.boot.impl.publish.spring.BeanFactoryUtils.registerBundleBeans;

/**
 * @author arik
 */
public class BundlePublisher {

    private static final Logger LOG = LoggerFactory.getBundleLogger( BundlePublisher.class );

    private final Object lock = new Object();

    private final Bundle bundle;

    private final OsgiSpringNamespacePlugin osgiSpringNamespacePlugin;

    private final RequirementFactory requirementFactory;

    private Collection<Requirement> requirements;

    private Collection<Requirement> satisfied;

    private Collection<Requirement> unsatisfied;

    private BundleApplicationContext applicationContext;

    private boolean started;

    public BundlePublisher( Bundle bundle, OsgiSpringNamespacePlugin osgiSpringNamespacePlugin ) {
        this.bundle = bundle;
        this.osgiSpringNamespacePlugin = osgiSpringNamespacePlugin;
        this.requirementFactory = new RequirementFactory( this, this.bundle, this.osgiSpringNamespacePlugin );
    }

    public BundleContext getBundleContext() {
        BundleContext bundleContext = this.bundle.getBundleContext();
        if( bundleContext == null ) {
            throw new IllegalStateException( "Cannot obtain bundle context - is bundle active? (" + BundleUtils.toString( this.bundle ) + ")" );
        }
        return bundleContext;
    }

    @SuppressWarnings( "ConstantConditions" )
    public void start() throws Exception {
        this.requirements = new HashSet<>();
        this.satisfied = new HashSet<>();
        this.unsatisfied = new HashSet<>();

        // open our requirements; every requirement starts in the 'unsatisfied' state until it notifies us differently
        for( Requirement requirement : this.requirementFactory.detectRequirements() ) {
            try {
                boolean satisfied = requirement.open();
                this.requirements.add( requirement );
                if( satisfied ) {
                    this.satisfied.add( requirement );
                } else {
                    this.unsatisfied.add( requirement );
                }

            } catch( Exception e ) {

                for( Requirement req : this.requirements ) {
                    req.close();
                }
                throw e;

            }
        }

        LOG.debug( "Tracking bundle '{}'", BundleUtils.toString( this.bundle ) );

        if( this.unsatisfied.isEmpty() ) {
            publish();
        }
        this.started = true;
    }

    public void stop() {
        LOG.debug( "No longer tracking bundle '{}'", BundleUtils.toString( this.bundle ) );
        this.started = false;

        // close application context
        close();

        // close our requirements
        for( Requirement requirement : this.requirements ) {
            requirement.close();
        }

        // remove references
        this.applicationContext = null;
        this.requirements = null;
        this.satisfied = null;
        this.unsatisfied = null;
    }

    public void markAsSatisfied( Requirement requirement, Object... state ) {
        synchronized( this.lock ) {

            this.unsatisfied.remove( requirement );
            this.satisfied.add( requirement );

            if( !this.started ) {

                // if we're not started yet, do nothing and return

            } else if( this.applicationContext != null ) {

                // already published - just re-onSatisfy this requirement on our published context
                try {
                    requirement.onSatisfy( this.applicationContext, state );
                } catch( Exception e ) {
                    LOG.error( "Requirement '{}' could not be satisfied: {}", requirement, e.getMessage(), e );
                }

            } else if( unsatisfied.isEmpty() ) {

                // we're not published - but all requirements are now satisfied - publish
                publish();

            }
        }
    }

    public void markAsUnsatisfied( Requirement requirement ) {
        synchronized( this.lock ) {

            this.unsatisfied.add( requirement );
            this.satisfied.remove( requirement );

            if( !this.started ) {

                // if we're not started yet, do nothing and return

            } else if( this.applicationContext != null ) {

                close();

            }
        }
    }

    private void publish() {
        try {
            LOG.info( "Publishing bundle '{}'", BundleUtils.toString( this.bundle ) );
            BundleApplicationContext applicationContext = new BundleApplicationContext( this.bundle );
            applicationContext.getBeanFactory().addBeanPostProcessor( new RequirementTargetsBeanPostProcessor() );
            registerBundleBeans( this.bundle, applicationContext, applicationContext.getClassLoader(), this.osgiSpringNamespacePlugin );
            applicationContext.refresh();

            for( Requirement requirement : this.requirements ) {
                requirement.onPublish( applicationContext );
            }

            this.applicationContext = applicationContext;

        } catch( Exception e ) {

            // publish failed - revert any effects requirements applied to the system (outside the app context)
            LOG.error( "Could not publish bundle '{}': {}", BundleUtils.toString( this.bundle ), e.getMessage(), e );
            close();

        }
    }

    private void close() {
        LOG.info( "Closing bundle '{}'", BundleUtils.toString( this.bundle ) );
        revertRequirements();
        if( this.applicationContext != null ) {
            try {
                this.applicationContext.close();
            } catch( Exception e ) {
                LOG.error( "Could not properly close application context for bundle '{}': {}", BundleUtils.toString( this.bundle ), e.getMessage(), e );
            } finally {
                this.applicationContext = null;
            }
        }
    }

    @SuppressWarnings( "ConstantConditions" )
    private void revertRequirements() {
        synchronized( this.lock ) {
            for( Requirement req : this.satisfied ) {
                try {
                    req.revert();
                } catch( Exception e ) {
                    LOG.error( "Requirement '{}' could not be reverted (after failed publish attempt): {}", req, e.getMessage(), e );
                }
            }
        }
    }

    private class RequirementTargetsBeanPostProcessor implements BeanPostProcessor {

        @Override
        public Object postProcessBeforeInitialization( Object bean, String beanName ) throws BeansException {
            synchronized( lock ) {
                for( Requirement requirement : satisfied ) {
                    try {
                        requirement.onInitBean( bean, beanName );
                    } catch( Exception e ) {
                        throw new BeanCreationException( beanName, e.getMessage(), e );
                    }
                }
            }
            return bean;
        }

        @Override
        public Object postProcessAfterInitialization( Object bean, String beanName ) throws BeansException {
            return bean;
        }
    }
}
