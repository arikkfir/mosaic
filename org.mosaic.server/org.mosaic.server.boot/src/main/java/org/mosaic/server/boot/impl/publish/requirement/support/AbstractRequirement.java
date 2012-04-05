package org.mosaic.server.boot.impl.publish.requirement.support;

import org.mosaic.server.boot.impl.publish.BundlePublisher;
import org.mosaic.server.boot.impl.publish.requirement.Requirement;
import org.osgi.framework.BundleContext;
import org.springframework.context.ApplicationContext;

/**
 * @author arik
 */
public abstract class AbstractRequirement implements Requirement {

    private final BundlePublisher publisher;

    protected AbstractRequirement( BundlePublisher publisher ) {
        this.publisher = publisher;
    }

    @Override
    public boolean open() throws Exception {
        // no-op
        return false;
    }

    @Override
    public void apply( ApplicationContext applicationContext, Object state ) throws Exception {
        // no-op
    }

    @Override
    public void applyInitial( ApplicationContext applicationContext ) throws Exception {
        // no-op
    }

    @Override
    public void revert() throws Exception {
        // no-op
    }

    @Override
    public void close() {
        // no-op
    }

    public BundlePublisher getPublisher() {
        return publisher;
    }

    protected BundleContext getBundleContext() {
        return this.publisher.getBundleContext();
    }

    protected void markAsSatisfied( Object state ) {
        this.publisher.markAsSatisfied( this, state );
    }

    protected void markAsUnsatisfied() {
        this.publisher.markAsUnsatisfied( this );
    }
}
