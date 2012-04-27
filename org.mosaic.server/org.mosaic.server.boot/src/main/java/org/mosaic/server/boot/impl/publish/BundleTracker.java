package org.mosaic.server.boot.impl.publish;

import java.util.*;
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

/**
 * @author arik
 */
public class BundleTracker {

    private static final Logger LOG = LoggerFactory.getLogger( BundleTracker.class );

    private final Object lock = new Object();

    private final Bundle bundle;

    private final OsgiSpringNamespacePlugin osgiSpringNamespacePlugin;

    private final RequirementFactory requirementFactory;

    private Collection<Requirement> requirements;

    private Collection<Requirement> satisfied;

    private Collection<Requirement> unsatisfied;

    private BundleApplicationContext applicationContext;

    private boolean tracking;

    private boolean publishing;

    public BundleTracker( Bundle bundle, OsgiSpringNamespacePlugin osgiSpringNamespacePlugin ) {
        this.bundle = bundle;
        this.osgiSpringNamespacePlugin = osgiSpringNamespacePlugin;
        this.requirementFactory = new RequirementFactory( this, this.bundle, this.osgiSpringNamespacePlugin );
    }

    public void track() throws Exception {
        LOG.info( "Tracking bundle '{}'", BundleUtils.toString( this.bundle ) );

        // initialize data structures
        this.requirements = new LinkedHashSet<>();
        this.satisfied = new LinkedHashSet<>();
        this.unsatisfied = new LinkedHashSet<>();

        // detect bundle requirements
        for( Requirement requirement : this.requirementFactory.detectRequirements() ) {
            try {
                if( requirement.track() ) {
                    this.satisfied.add( requirement );
                } else {
                    this.unsatisfied.add( requirement );
                }
                this.requirements.add( requirement );

            } catch( Exception e ) {
                for( Requirement req : getReversedRequirements() ) {
                    req.untrack();
                }
                throw e;
            }
        }
        this.tracking = true;

        // if all are satisfied, publish; otherwise, it will be published later when the requirements will be satisfied (other threads)
        if( this.unsatisfied.isEmpty() ) {
            publish();
        } else {
            LOG.info( "Bundle '{}' has unsatisfied dependencies, and will be published when they are satisfied. Dependencies are:", BundleUtils.toString( this.bundle ) );
            for( Requirement requirement : this.unsatisfied ) {
                LOG.info( "    {}", requirement );
            }
        }
    }

    public boolean isTracking() {
        return tracking;
    }

    public boolean isPublished() {
        return this.applicationContext != null;
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public Collection<Requirement> getSatisfiedRequirements() {
        return this.satisfied;
    }

    public Collection<Requirement> getUnsatisfiedRequirements() {
        return this.unsatisfied;
    }

    public void untrack() {

        // un-publish bundle
        unpublish();

        // untrack our requirements
        this.tracking = false;
        for( Requirement requirement : getReversedRequirements() ) {
            requirement.untrack();
        }

        // remove references
        this.applicationContext = null;
        this.requirements = null;
        this.satisfied = null;
        this.unsatisfied = null;

    }

    public BundleContext getBundleContext() {
        return this.bundle.getBundleContext();
    }

    public void markAsSatisfied( Requirement requirement, Object... state ) {
        synchronized( this.lock ) {
            this.unsatisfied.remove( requirement );
            this.satisfied.add( requirement );
            if( this.tracking ) {
                if( this.applicationContext != null ) {

                    // already published - just re-onSatisfy this requirement on our published context
                    requirement.onSatisfy( this.applicationContext, state );

                } else if( unsatisfied.isEmpty() ) {

                    // we're not published - but all requirements are now satisfied - publish
                    publish();

                }
            }
        }
    }

    public boolean isSatisfied( Requirement requirement ) {
        return this.satisfied.contains( requirement );
    }

    public void markAsUnsatisfied( Requirement requirement ) {
        synchronized( this.lock ) {
            this.unsatisfied.add( requirement );
            this.satisfied.remove( requirement );
            if( this.tracking ) {

                if( this.applicationContext != null ) {
                    unpublish();
                }

            }
        }
    }

    private void publish() {
        if( !this.publishing ) {
            this.publishing = true;
            try {
                LOG.info( "Publishing bundle '{}'", BundleUtils.toString( this.bundle ) );
                BundleApplicationContext applicationContext = new BundleApplicationContext( this.bundle, this.osgiSpringNamespacePlugin );
                applicationContext.getBeanFactory().addBeanPostProcessor( new RequirementTargetsBeanPostProcessor() );
                applicationContext.refresh();

                for( Requirement requirement : this.requirements ) {
                    requirement.publish( applicationContext );
                }

                this.applicationContext = applicationContext;

            } catch( Exception e ) {

                // publish failed - revert any effects requirements applied to the system (outside the app context)
                LOG.error( "Could not publish bundle '{}': {}", BundleUtils.toString( this.bundle ), e.getMessage(), e );
                unpublish();

            } finally {
                this.publishing = false;
            }
        }
    }

    private void unpublish() {
        for( Requirement requirement : this.satisfied ) {
            requirement.unpublish();
        }

        if( this.applicationContext != null ) {
            try {
                this.applicationContext.close();
            } catch( Exception e ) {
                LOG.error( "Could not properly close application context for bundle '{}': {}", BundleUtils.toString( this.bundle ), e.getMessage(), e );
            } finally {
                this.applicationContext = null;
            }
        }

        LOG.info( "Unpublished bundle '{}'", BundleUtils.toString( this.bundle ) );
    }

    private Collection<Requirement> getReversedRequirements() {
        if( this.requirements == null ) {
            return Collections.emptyList();
        }
        List<Requirement> reversed = new LinkedList<>( this.requirements );
        Collections.reverse( reversed );
        return reversed;
    }

    private class RequirementTargetsBeanPostProcessor implements BeanPostProcessor {

        @Override
        public Object postProcessBeforeInitialization( Object bean, String beanName ) throws BeansException {
            synchronized( lock ) {
                for( Requirement requirement : satisfied ) {
                    try {
                        if( requirement.isBeanPublishable( bean, beanName ) ) {
                            requirement.publishBean( bean, beanName );
                        }
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
