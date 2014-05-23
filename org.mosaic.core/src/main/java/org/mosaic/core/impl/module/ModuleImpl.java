package org.mosaic.core.impl.module;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import org.mosaic.core.*;
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
        return ToStringHelper.create( "Module" )
                             .add( "id", this.bundle.getBundleId() )
                             .add( "name", this.bundle.getSymbolicName() )
                             .toString();
    }

    @Override
    public long getId()
    {
        this.lock.acquireReadLock();
        try
        {
            return this.bundle.getBundleId();
        }
        finally
        {
            this.lock.releaseReadLock();
        }
    }

    @Nullable
    @Override
    public Path getPath()
    {
        this.lock.acquireReadLock();
        try
        {
            String location = this.bundle.getLocation();
            if( location.startsWith( "file:" ) )
            {
                return Paths.get( location.substring( "file:".length() ) );
            }
            else
            {
                return null;
            }
        }
        finally
        {
            this.lock.releaseReadLock();
        }
    }

    @Nonnull
    @Override
    public ModuleState getState()
    {
        this.lock.acquireReadLock();
        try
        {
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
        }
        finally
        {
            this.lock.releaseReadLock();
        }
    }

    @Nullable
    @Override
    public ModuleRevisionImpl getCurrentRevision()
    {
        this.lock.acquireReadLock();
        try
        {
            List<BundleRevision> revisions = this.bundle.adapt( BundleRevisions.class ).getRevisions();
            if( revisions.isEmpty() )
            {
                return null;
            }
            else
            {
                return this.revisions.get( revisions.get( 0 ) );
            }
        }
        finally
        {
            this.lock.releaseReadLock();
        }
    }

    @Nonnull
    @Override
    public Collection<ModuleRevision> getRevisions()
    {
        this.lock.acquireReadLock();
        try
        {
            return Collections.<ModuleRevision>unmodifiableCollection( this.revisions.values() );
        }
        finally
        {
            this.lock.releaseReadLock();
        }
    }

    @Nullable
    @Override
    public ModuleRevision getRevision( long revisionId )
    {
        this.lock.acquireReadLock();
        try
        {
            for( ModuleRevisionImpl moduleRevision : this.revisions.values() )
            {
                if( moduleRevision.getId() == revisionId )
                {
                    return moduleRevision;
                }
            }
            return null;
        }
        finally
        {
            this.lock.releaseReadLock();
        }
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
    public void start()
    {
        this.lock.acquireWriteLock();
        try
        {
            this.bundle.start();
        }
        catch( Throwable e )
        {
            throw new ModuleStartException( "could not start module " + this, e, this );
        }
        finally
        {
            this.lock.releaseWriteLock();
        }
    }

    @Override
    public void refresh()
    {
        this.lock.acquireWriteLock();
        try
        {
            this.bundle.update();
        }
        catch( Throwable e )
        {
            throw new ModuleRefreshException( "could not refresh module " + this, e, this );
        }
        finally
        {
            this.lock.releaseWriteLock();
        }
    }

    @Override
    public void stop()
    {
        this.lock.acquireWriteLock();
        try
        {
            this.bundle.stop();
        }
        catch( Throwable e )
        {
            throw new ModuleStartException( "could not stop module " + this, e, this );
        }
        finally
        {
            this.lock.releaseWriteLock();
        }
    }

    @Override
    public void uninstall()
    {
        this.lock.acquireWriteLock();
        try
        {
            this.bundle.uninstall();
        }
        catch( Throwable e )
        {
            throw new ModuleUninstallException( "could not uninstall module " + this, e, this );
        }
        finally
        {
            this.lock.releaseWriteLock();
        }
    }

    @Nonnull
    Logger getLogger()
    {
        return this.logger;
    }

    @Nonnull
    ReadWriteLock getLock()
    {
        return this.lock;
    }

    @Nonnull
    Bundle getBundle()
    {
        return this.bundle;
    }

    @Nullable
    ModuleRevisionImpl getRevision( @Nonnull BundleRevision bundleRevision )
    {
        this.lock.acquireReadLock();
        try
        {
            return this.revisions.get( bundleRevision );
        }
        finally
        {
            this.lock.releaseReadLock();
        }
    }

    void syncBundleRevisions()
    {
        this.lock.acquireWriteLock();
        try
        {
            for( BundleRevision bundleRevision : this.bundle.adapt( BundleRevisions.class ).getRevisions() )
            {
                if( !this.revisions.containsKey( bundleRevision ) )
                {
                    this.revisions.put( bundleRevision, new ModuleRevisionImpl( this, bundleRevision ) );
                }
            }
        }
        finally
        {
            this.lock.releaseWriteLock();
        }
    }

    void bundleInstalled()
    {
        this.lock.acquireWriteLock();
        try
        {
            this.logger.info( "INSTALLING {}", this );
            syncBundleRevisions();
            this.logger.info( "INSTALLED {}", this );
        }
        finally
        {
            this.lock.releaseWriteLock();
        }
    }

    void bundleResolved()
    {
        this.lock.acquireWriteLock();
        try
        {
            this.logger.info( "RESOLVING {}", this );

            syncBundleRevisions();

            ModuleRevisionImpl revision = getCurrentRevision();
            if( revision != null )
            {
                revision.revisionResolved();
            }

            this.logger.info( "RESOLVED {}", this );
        }
        finally
        {
            this.lock.releaseWriteLock();
        }
    }

    void bundleStarting()
    {
        this.lock.acquireWriteLock();
        try
        {
            this.logger.info( "STARTING {}", this );

            ModuleRevisionImpl revision = getCurrentRevision();
            if( revision != null )
            {
                revision.revisionStarting();
            }
        }
        finally
        {
            this.lock.releaseWriteLock();
        }
    }

    void bundleStarted()
    {
        this.lock.acquireWriteLock();
        try
        {
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
        }
        finally
        {
            this.lock.releaseWriteLock();
        }
    }

    void bundleStopping()
    {
        this.lock.acquireWriteLock();
        try
        {
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
        }
        finally
        {
            this.lock.releaseWriteLock();
        }
    }

    void bundleStopped()
    {
        this.lock.acquireWriteLock();
        try
        {
            ModuleRevisionImpl revision = getCurrentRevision();
            if( revision != null )
            {
                revision.revisionStopped();
            }

            this.logger.info( "STOPPED {}", this );
        }
        finally
        {
            this.lock.releaseWriteLock();
        }
    }

    void bundleUpdated()
    {
        this.lock.acquireWriteLock();
        try
        {
            syncBundleRevisions();
            this.logger.info( "UPDATED {}", this );
        }
        finally
        {
            this.lock.releaseWriteLock();
        }
    }

    void bundleUnresolved()
    {
        this.lock.acquireWriteLock();
        try
        {
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
        }
        finally
        {
            this.lock.releaseWriteLock();
        }
    }

    void bundleUninstalled()
    {
        this.lock.acquireWriteLock();
        try
        {
            this.logger.info( "UNINSTALLED {}", this );
        }
        finally
        {
            this.lock.releaseWriteLock();
        }
    }
}
