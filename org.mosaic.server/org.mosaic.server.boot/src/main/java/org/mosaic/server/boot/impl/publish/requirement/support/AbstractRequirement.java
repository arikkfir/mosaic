package org.mosaic.server.boot.impl.publish.requirement.support;

import org.mosaic.logging.Logger;
import org.mosaic.logging.LoggerFactory;
import org.mosaic.osgi.util.BundleUtils;
import org.mosaic.server.boot.impl.publish.BundleTracker;
import org.mosaic.server.boot.impl.publish.requirement.Requirement;
import org.osgi.framework.BundleContext;
import org.springframework.context.ApplicationContext;

/**
 * @author arik
 */
public abstract class AbstractRequirement implements Requirement {

    private static final Logger LOG = LoggerFactory.getBundleLogger( AbstractRequirement.class );

    private final BundleContext bundleContext;

    private final String bundleName;

    private final BundleTracker tracker;

    protected AbstractRequirement( BundleTracker tracker ) {
        this.bundleContext = tracker.getBundleContext();
        this.tracker = tracker;
        this.bundleName = BundleUtils.toString( this.bundleContext.getBundle() );
    }

    protected String getBundleName() {
        return bundleName;
    }

    @Override
    public final boolean track() throws Exception {
        LOG.debug( "Tracking requirement '{}' of bundle '{}'", this, this.bundleName );
        return trackInternal();
    }

    protected boolean trackInternal() throws Exception {
        return false;
    }

    @Override
    public final void untrack() {
        LOG.debug( "Untracking requirement '{}' of bundle '{}'", this, this.bundleName );
        try {
            untrackInternal();
        } catch( Exception e ) {
            LOG.error( "Error while untracking requirement '{}' of bundle '{}': {}", this, this.bundleName, e.getMessage(), e );
        }
    }

    protected void untrackInternal() throws Exception {
        // no-op
    }

    @Override
    public final void publish( ApplicationContext applicationContext ) throws Exception {
        LOG.debug( "Publishing requirement '{}' of bundle '{}'", this, this.bundleName );
        publishInternal( applicationContext );
    }

    protected void publishInternal( ApplicationContext applicationContext ) throws Exception {
        // no-op
    }

    @Override
    public final void unpublish() {
        LOG.debug( "Unpublishing requirement '{}' of bundle '{}'", this, this.bundleName );
        try {
            unpublishInternal();
        } catch( Exception e ) {
            LOG.error( "Error while unpublishing requirement '{}' of bundle '{}': {}", this, this.bundleName, e.getMessage(), e );
        }
    }

    protected void unpublishInternal() throws Exception {
        // no-op
    }

    @Override
    public boolean isBeanPublishable( Object bean, String beanName ) {
        return false;
    }

    @Override
    public final void publishBean( Object bean, String beanName ) throws Exception {
        LOG.debug( "Requirement '{}' in bundle '{}' is initializing bean '{}'", this, this.bundleName, beanName );
        publishBeanInternal( bean, beanName );
    }

    protected void publishBeanInternal( Object bean, String beanName ) throws Exception {
        // no-op
    }

    @Override
    public void onSatisfy( ApplicationContext applicationContext, Object... state ) {
        try {
            onSatisfyInternal( applicationContext, state );
        } catch( Exception e ) {
            LOG.error( "Requirement '{}' could not be satisfied: {}", this, e.getMessage(), e );
        }
    }

    protected void onSatisfyInternal( ApplicationContext applicationContext, Object... state ) throws Exception {
        // no-op
    }

    protected BundleContext getBundleContext() {
        return this.bundleContext;
    }

    protected void markAsSatisfied( Object... state ) {
        if( !this.tracker.isSatisfied( this ) ) {
            LOG.debug( "Requirement '{}' of bundle '{}' is now satisfied", this, this.bundleName );
        }
        this.tracker.markAsSatisfied( this, state );
    }

    protected void markAsUnsatisfied() {
        if( this.tracker.isSatisfied( this ) ) {
            LOG.debug( "Requirement '{}' of bundle '{}' is now un-satisfied", this, this.bundleName );
        }
        this.tracker.markAsUnsatisfied( this );
    }
}
