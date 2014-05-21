package org.mosaic.core.impl;

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

/**
 * @author arik
 */
class ModuleImpl implements Module
{
    @Nonnull
    private final ServerImpl server;

    @Nonnull
    private final Bundle bundle;

    @Nonnull
    private final Map<BundleRevision, ModuleRevisionImpl> revisions = new WeakHashMap<>();

    ModuleImpl( @Nonnull ServerImpl server, @Nonnull Bundle bundle )
    {
        this.server = server;
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
        this.server.getLock().acquireReadLock();
        try
        {
            return this.bundle.getBundleId();
        }
        finally
        {
            this.server.getLock().releaseReadLock();
        }
    }

    @Nullable
    @Override
    public Path getPath()
    {
        this.server.getLock().acquireReadLock();
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
            this.server.getLock().releaseReadLock();
        }
    }

    @Nonnull
    @Override
    public ModuleState getState()
    {
        this.server.getLock().acquireReadLock();
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
            this.server.getLock().releaseReadLock();
        }
    }

    @Nullable
    @Override
    public ModuleRevisionImpl getCurrentRevision()
    {
        this.server.getLock().acquireReadLock();
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
            this.server.getLock().releaseReadLock();
        }
    }

    @Nonnull
    @Override
    public Collection<ModuleRevision> getRevisions()
    {
        this.server.getLock().acquireReadLock();
        try
        {
            return Collections.<ModuleRevision>unmodifiableCollection( this.revisions.values() );
        }
        finally
        {
            this.server.getLock().releaseReadLock();
        }
    }

    @Nullable
    @Override
    public ModuleRevision getRevision( long revisionId )
    {
        this.server.getLock().acquireReadLock();
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
            this.server.getLock().releaseReadLock();
        }
    }

    @Nonnull
    @Override
    public <ServiceType> ServiceRegistration<ServiceType> registerService( @Nonnull Class<ServiceType> type,
                                                                           @Nonnull ServiceType service,
                                                                           @Nonnull ServiceProperty... properties )
    {
        return this.server.getServiceManager().registerService( this, type, service, properties );
    }

    @Nonnull
    @Override
    public <ServiceType> ServiceTracker<ServiceType> createServiceTracker( @Nonnull Class<ServiceType> type,
                                                                           @Nonnull ServiceProperty... properties )
    {
        return this.server.getServiceManager().createServiceTracker( type, properties );
    }

    @Override
    public void start()
    {
        this.server.getLock().acquireWriteLock();
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
            this.server.getLock().releaseWriteLock();
        }
    }

    @Override
    public void refresh()
    {
        this.server.getLock().acquireWriteLock();
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
            this.server.getLock().releaseWriteLock();
        }
    }

    @Override
    public void stop()
    {
        this.server.getLock().acquireWriteLock();
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
            this.server.getLock().releaseWriteLock();
        }
    }

    @Override
    public void uninstall()
    {
        this.server.getLock().acquireWriteLock();
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
            this.server.getLock().releaseWriteLock();
        }
    }

    @Nonnull
    Bundle getBundle()
    {
        return this.bundle;
    }

    @Nonnull
    ReadWriteLock getLock()
    {
        return this.server.getLock();
    }

    @Nonnull
    ServerImpl getServer()
    {
        return this.server;
    }

    @Nullable
    ModuleRevisionImpl getRevision( @Nonnull BundleRevision bundleRevision )
    {
        this.server.getLock().acquireReadLock();
        try
        {
            return this.revisions.get( bundleRevision );
        }
        finally
        {
            this.server.getLock().releaseReadLock();
        }
    }

    void syncBundleRevisions()
    {
        this.server.getLock().acquireWriteLock();
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
            this.server.getLock().releaseWriteLock();
        }
    }

    void bundleInstalled()
    {
        this.server.getLock().acquireWriteLock();
        try
        {
            this.server.getLogger().info( "INSTALLING {}", this );
            syncBundleRevisions();
            this.server.getLogger().info( "INSTALLED {}", this );
        }
        finally
        {
            this.server.getLock().releaseWriteLock();
        }
    }

    void bundleResolved()
    {
        this.server.getLock().acquireWriteLock();
        try
        {
            this.server.getLogger().info( "RESOLVING {}", this );

            syncBundleRevisions();

            ModuleRevisionImpl revision = getCurrentRevision();
            if( revision != null )
            {
                revision.revisionResolved();
            }

            this.server.getLogger().info( "RESOLVED {}", this );
        }
        finally
        {
            this.server.getLock().releaseWriteLock();
        }
    }

    void bundleStarting()
    {
        this.server.getLock().acquireWriteLock();
        try
        {
            this.server.getLogger().info( "STARTING {}", this );

            ModuleRevisionImpl revision = getCurrentRevision();
            if( revision != null )
            {
                revision.revisionStarting();
            }
        }
        finally
        {
            this.server.getLock().releaseWriteLock();
        }
    }

    void bundleStarted()
    {
        this.server.getLock().acquireWriteLock();
        try
        {
            ModuleRevisionImpl revision = getCurrentRevision();
            if( revision != null )
            {
                revision.revisionStarted();
            }

            this.server.getLogger().info( "STARTED {}", this );

            if( revision != null )
            {
                revision.activate();
            }
        }
        finally
        {
            this.server.getLock().releaseWriteLock();
        }
    }

    void bundleStopping()
    {
        this.server.getLock().acquireWriteLock();
        try
        {
            ModuleRevisionImpl revision = getCurrentRevision();
            if( revision != null )
            {
                revision.deactivate();
            }

            this.server.getLogger().info( "STOPPING {}", this );

            if( revision != null )
            {
                revision.revisionStopping();
            }
        }
        finally
        {
            this.server.getLock().releaseWriteLock();
        }
    }

    void bundleStopped()
    {
        this.server.getLock().acquireWriteLock();
        try
        {
            ModuleRevisionImpl revision = getCurrentRevision();
            if( revision != null )
            {
                revision.revisionStopped();
            }

            this.server.getLogger().info( "STOPPED {}", this );
        }
        finally
        {
            this.server.getLock().releaseWriteLock();
        }
    }

    void bundleUpdated()
    {
        this.server.getLock().acquireWriteLock();
        try
        {
            syncBundleRevisions();
            this.server.getLogger().info( "UPDATED {}", this );
        }
        finally
        {
            this.server.getLock().releaseWriteLock();
        }
    }

    void bundleUnresolved()
    {
        this.server.getLock().acquireWriteLock();
        try
        {
            this.server.getLogger().info( "UNRESOLVING {}", this );

            for( BundleRevision bundleRevision : this.bundle.adapt( BundleRevisions.class ).getRevisions() )
            {
                ModuleRevisionImpl moduleRevision = getRevision( bundleRevision );
                if( moduleRevision != null && bundleRevision.getWiring() == null )
                {
                    moduleRevision.revisionUnresolved();
                }
            }

            this.server.getLogger().info( "UNRESOLVED {}", this );
        }
        finally
        {
            this.server.getLock().releaseWriteLock();
        }
    }

    void bundleUninstalled()
    {
        this.server.getLock().acquireWriteLock();
        try
        {
            this.server.getLogger().info( "UNINSTALLED {}", this );
        }
        finally
        {
            this.server.getLock().releaseWriteLock();
        }
    }
}
