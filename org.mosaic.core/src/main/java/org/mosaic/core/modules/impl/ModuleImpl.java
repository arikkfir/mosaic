package org.mosaic.core.modules.impl;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import org.mosaic.core.modules.*;
import org.mosaic.core.services.*;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.core.util.base.ToStringHelper;
import org.mosaic.core.util.concurrency.ReadWriteLock;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleRevisions;
import org.slf4j.Logger;

/**
 * @author arik
 */
class ModuleImpl implements Module
{
    @Nonnull
    private final Logger logger;

    @Nonnull
    private final ReadWriteLock lock;

    @Nonnull
    private final ServiceManager serviceManager;

    @Nonnull
    private final Bundle bundle;

    @Nonnull
    private final Map<BundleRevision, ModuleRevisionImpl> revisions = new WeakHashMap<>();

    ModuleImpl( @Nonnull Logger logger,
                @Nonnull ReadWriteLock lock,
                @Nonnull ServiceManager serviceManager,
                @Nonnull Bundle bundle )
    {
        this.logger = logger;
        this.lock = lock;
        this.serviceManager = serviceManager;
        this.bundle = bundle;
    }

    @Override
    public String toString()
    {
        return this.lock.read( () -> ToStringHelper.create( "Module" )
                                                   .add( "id", this.bundle.getBundleId() )
                                                   .add( "name", this.bundle.getSymbolicName() )
                                                   .toString() );
    }

    @Override
    public long getId()
    {
        return this.lock.read( this.bundle::getBundleId );
    }

    @Nullable
    @Override
    public Path getPath()
    {
        return this.lock.read( () -> {
            String location = this.bundle.getLocation();
            return location.startsWith( "file:" ) ? Paths.get( location.substring( "file:".length() ) ) : null;
        } );
    }

    @Nonnull
    @Override
    public ModuleState getState()
    {
        return this.lock.read( () -> {
            int state = this.bundle.getState();
            switch( state )
            {
                case Bundle.INSTALLED:
                    return ModuleState.INSTALLED;

                case Bundle.RESOLVED:
                    return ModuleState.RESOLVED;

                case Bundle.STARTING:
                    return ModuleState.STARTING;

                case Bundle.ACTIVE:
                    ModuleRevisionImpl currentRevision = getCurrentRevision();
                    return currentRevision != null && currentRevision.isActivated() ? ModuleState.ACTIVE : ModuleState.STARTED;

                case Bundle.STOPPING:
                    return ModuleState.STOPPING;

                case Bundle.UNINSTALLED:
                    return ModuleState.UNINSTALLED;

                default:
                    throw new IllegalStateException( "unknown bundle state: " + state );
            }
        } );
    }

    @Nullable
    @Override
    public ModuleRevisionImpl getCurrentRevision()
    {
        return this.lock.read( () -> {
            List<BundleRevision> revisions = this.bundle.adapt( BundleRevisions.class ).getRevisions();
            if( revisions.isEmpty() )
            {
                return null;
            }
            else
            {
                return this.revisions.get( revisions.get( 0 ) );
            }
        } );
    }

    @Nonnull
    @Override
    public Collection<ModuleRevision> getRevisions()
    {
        return this.lock.read( () -> Collections.<ModuleRevision>unmodifiableCollection( this.revisions.values() ) );
    }

    @Nullable
    @Override
    public ModuleRevision getRevision( long revisionId )
    {
        return this.lock.read( () -> {
            for( ModuleRevisionImpl moduleRevision : this.revisions.values() )
            {
                if( moduleRevision.getId() == revisionId )
                {
                    return moduleRevision;
                }
            }
            return null;
        } );
    }

    @Nonnull
    @Override
    public <ServiceType> ServiceRegistration<ServiceType> registerService( @Nonnull Class<ServiceType> type,
                                                                           @Nonnull ServiceType service,
                                                                           @Nonnull ServiceProperty... properties )
    {
        return this.serviceManager.registerService( this, type, service, properties );
    }

    @Override
    public <ServiceType> ServiceListenerRegistration<ServiceType> addServiceListener( @Nonnull ServiceListener<ServiceType> listener,
                                                                                      @Nonnull Class<ServiceType> type,
                                                                                      @Nonnull ServiceProperty... properties )
    {
        return this.serviceManager.addListener( this, listener, type, properties );
    }

    @Override
    public <ServiceType> ServiceListenerRegistration<ServiceType> addServiceListener( @Nonnull ServiceManager.ServiceRegisteredAction<ServiceType> onRegister,
                                                                                      @Nonnull ServiceManager.ServiceUnregisteredAction<ServiceType> onUnregister,
                                                                                      @Nonnull Class<ServiceType> type,
                                                                                      @Nonnull ServiceProperty... properties )
    {
        return this.serviceManager.addListener( this, onRegister, onUnregister, type, properties );
    }

    @Override
    public <ServiceType> ServiceListenerRegistration<ServiceType> addWeakServiceListener( @Nonnull ServiceListener<ServiceType> listener,
                                                                                          @Nonnull Class<ServiceType> type,
                                                                                          @Nonnull ServiceProperty... properties )
    {
        return this.serviceManager.addWeakListener( this, listener, type, properties );
    }

    @Nonnull
    @Override
    public <ServiceType> ServiceTracker<ServiceType> createServiceTracker( @Nonnull Class<ServiceType> type,
                                                                           @Nonnull ServiceProperty... properties )
    {
        return this.serviceManager.createServiceTracker( this, type, properties );
    }

    @Override
    public void start()
    {
        this.lock.write( () -> {
            try
            {
                this.bundle.start();
            }
            catch( Throwable e )
            {
                throw new ModuleStartException( "could not start module " + this, e, this );
            }
        } );
    }

    @Override
    public void refresh()
    {
        this.lock.write( () -> {
            try
            {
                this.bundle.update();
            }
            catch( Throwable e )
            {
                throw new ModuleRefreshException( "could not refresh module " + this, e, this );
            }
        } );
    }

    @Override
    public void stop()
    {
        this.lock.write( () -> {
            try
            {
                this.bundle.stop();
            }
            catch( Throwable e )
            {
                throw new ModuleStartException( "could not stop module " + this, e, this );
            }
        } );
    }

    @Override
    public void uninstall()
    {
        this.lock.write( () -> {
            try
            {
                this.bundle.uninstall();
            }
            catch( Throwable e )
            {
                throw new ModuleUninstallException( "could not uninstall module " + this, e, this );
            }
        } );
    }

    @Nonnull
    Logger getLogger()
    {
        return this.lock.read( () -> this.logger );
    }

    @Nonnull
    ReadWriteLock getLock()
    {
        return this.lock.read( () -> this.lock );
    }

    @Nonnull
    Bundle getBundle()
    {
        return this.lock.read( () -> this.bundle );
    }

    @Nullable
    ModuleRevisionImpl getRevision( @Nonnull BundleRevision bundleRevision )
    {
        return this.lock.read( () -> this.revisions.get( bundleRevision ) );
    }

    void syncBundleRevisions()
    {
        this.lock.write( () -> this.bundle.adapt( BundleRevisions.class ).getRevisions()
                                          .stream()
                                          .filter( rev -> !this.revisions.containsKey( rev ) )
                                          .forEach( rev -> this.revisions.put( rev, new ModuleRevisionImpl( this, rev ) ) ) );
    }

    void bundleInstalled()
    {
        this.lock.write( () -> {
            this.logger.info( "INSTALLING {}", this );
            syncBundleRevisions();
            this.logger.info( "INSTALLED {}", this );
        } );
    }

    void bundleResolved()
    {
        this.lock.write( () -> {
            this.logger.info( "RESOLVING {}", this );

            syncBundleRevisions();

            ModuleRevisionImpl revision = getCurrentRevision();
            if( revision != null )
            {
                revision.revisionResolved();
            }

            this.logger.info( "RESOLVED {}", this );
        } );
    }

    void bundleStarting()
    {
        this.lock.write( () -> {
            this.logger.info( "STARTING {}", this );

            ModuleRevisionImpl revision = getCurrentRevision();
            if( revision != null )
            {
                revision.revisionStarting();
            }
        } );
    }

    void bundleStarted()
    {
        this.lock.write( () -> {
            ModuleRevisionImpl revision = getCurrentRevision();
            if( revision != null )
            {
                revision.revisionStarted();
            }

            this.logger.info( "STARTED {}", this );

            if( revision != null )
            {
                revision.activate();
            }
        } );
    }

    void bundleStopping()
    {
        this.lock.write( () -> {
            ModuleRevisionImpl revision = getCurrentRevision();
            if( revision != null )
            {
                revision.deactivate();
            }

            this.logger.info( "STOPPING {}", this );

            if( revision != null )
            {
                revision.revisionStopping();
            }
        } );
    }

    void bundleStopped()
    {
        this.lock.write( () -> {
            ModuleRevisionImpl revision = getCurrentRevision();
            if( revision != null )
            {
                revision.revisionStopped();
            }

            this.logger.info( "STOPPED {}", this );
        } );
    }

    void bundleUpdated()
    {
        this.lock.write( () -> {
            syncBundleRevisions();
            this.logger.info( "UPDATED {}", this );
        } );
    }

    void bundleUnresolved()
    {
        this.lock.write( () -> {
            this.logger.info( "UNRESOLVING {}", this );

            for( BundleRevision bundleRevision : this.bundle.adapt( BundleRevisions.class ).getRevisions() )
            {
                ModuleRevisionImpl moduleRevision = getRevision( bundleRevision );
                if( moduleRevision != null && bundleRevision.getWiring() == null )
                {
                    moduleRevision.revisionUnresolved();
                }
            }

            this.logger.info( "UNRESOLVED {}", this );
        } );
    }

    void bundleUninstalled()
    {
        this.lock.write( () -> this.logger.info( "UNINSTALLED {}", this ) );
    }
}
