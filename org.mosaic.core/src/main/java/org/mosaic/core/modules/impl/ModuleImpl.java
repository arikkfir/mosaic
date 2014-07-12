package org.mosaic.core.modules.impl;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import org.mosaic.core.launcher.impl.ServerImpl;
import org.mosaic.core.modules.*;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleRevisions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

/**
 * @author arik
 */
class ModuleImpl implements Module
{
    @Nonnull
    private static final Logger LOG = LoggerFactory.getLogger( ModuleImpl.class );

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
        return format( "%s@%s[%d]", this.bundle.getSymbolicName(), this.bundle.getVersion(), getId() );
    }

    @Override
    public long getId()
    {
        return this.server.getLock().read( this.bundle::getBundleId );
    }

    @Nullable
    @Override
    public Path getPath()
    {
        return this.server.getLock().read( () -> {
            String location = this.bundle.getLocation();
            return location.startsWith( "file:" ) ? Paths.get( location.substring( "file:".length() ) ) : null;
        } );
    }

    @Nonnull
    @Override
    public ModuleState getState()
    {
        return this.server.getLock().read( () -> {
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
        return this.server.getLock().read( () -> this.revisions.get( this.bundle.adapt( BundleRevision.class ) ) );
    }

    @Nonnull
    @Override
    public Collection<ModuleRevision> getRevisions()
    {
        return this.server.getLock().read( () -> Collections.<ModuleRevision>unmodifiableCollection( this.revisions.values() ) );
    }

    @Nullable
    @Override
    public ModuleRevision getRevision( long revisionId )
    {
        return this.server.getLock().read( () -> this.revisions.values()
                                                               .stream()
                                                               .filter( moduleRevision -> moduleRevision.getId() == revisionId )
                                                               .findFirst().orElse( null ) );
    }

    @Override
    public void start()
    {
        try
        {
            this.bundle.start();
        }
        catch( Throwable e )
        {
            throw new ModuleStartException( "could not start module " + this, e, this );
        }
    }

    @Override
    public void refresh()
    {
        try
        {
            this.bundle.update();
        }
        catch( Throwable e )
        {
            throw new ModuleRefreshException( "could not refresh module " + this, e, this );
        }
    }

    @Override
    public void stop()
    {
        stop( true );
    }

    public void stop( boolean persistent )
    {
        try
        {
            this.bundle.stop( persistent ? 0 : Bundle.STOP_TRANSIENT );
        }
        catch( Throwable e )
        {
            throw new ModuleStartException( "could not stop module " + this, e, this );
        }
    }

    @Override
    public void uninstall()
    {
        try
        {
            this.bundle.uninstall();
        }
        catch( Throwable e )
        {
            throw new ModuleUninstallException( "could not uninstall module " + this, e, this );
        }
    }

    @Nonnull
    public Bundle getBundle()
    {
        return this.server.getLock().read( () -> this.bundle );
    }

    void syncBundleRevisions()
    {
        this.server.getLock().write( () -> this.bundle.adapt( BundleRevisions.class )
                                                      .getRevisions()
                                                      .stream()
                                                      .filter( rev -> !this.revisions.containsKey( rev ) )
                                                      .forEach( rev -> this.revisions.put( rev, new ModuleRevisionImpl( this.server, this, rev ) ) ) );
    }

    void bundleInstalled()
    {
        this.server.getLock().write( () -> {
            LOG.info( "INSTALLING {}", this );
            syncBundleRevisions();
            LOG.info( "INSTALLED {}", this );

            this.server.getModuleManager().notifyModuleListeners( listener -> listener.moduleInstalled( this ) );
        } );
    }

    void bundleResolved()
    {
        this.server.getLock().write( () -> {
            LOG.info( "RESOLVING {}", this );

            syncBundleRevisions();

            ModuleRevisionImpl revision = getCurrentRevision();
            if( revision != null )
            {
                revision.revisionResolved();
            }

            LOG.info( "RESOLVED {}", this );

            this.server.getModuleManager().notifyModuleListeners( listener -> listener.moduleResolved( this ) );
        } );
    }

    void bundleStarting()
    {
        this.server.getLock().write( () -> {
            LOG.info( "STARTING {}", this );

            ModuleRevisionImpl revision = getCurrentRevision();
            if( revision != null )
            {
                revision.revisionStarting();
            }
        } );
    }

    void bundleStarted()
    {
        this.server.getLock().write( () -> {
            ModuleRevisionImpl revision = getCurrentRevision();
            if( revision != null )
            {
                revision.revisionStarted();
            }

            LOG.info( "STARTED {}", this );

            if( revision != null )
            {
                revision.activate();
            }

            this.server.getModuleManager().notifyModuleListeners( listener -> listener.moduleStarted( this ) );
        } );
    }

    void bundleStopping()
    {
        this.server.getLock().write( () -> {
            ModuleRevisionImpl revision = getCurrentRevision();
            if( revision != null )
            {
                revision.deactivate();
            }

            LOG.info( "STOPPING {}", this );

            if( revision != null )
            {
                revision.revisionStopping();
            }
        } );
    }

    void bundleStopped()
    {
        this.server.getLock().write( () -> {
            ModuleRevisionImpl revision = getCurrentRevision();
            if( revision != null )
            {
                revision.revisionStopped();
            }

            LOG.info( "STOPPED {}", this );

            this.server.getModuleManager().notifyModuleListeners( listener -> listener.moduleStopped( this ) );
        } );
    }

    void bundleUpdated()
    {
        this.server.getLock().write( () -> {
            syncBundleRevisions();
            LOG.info( "UPDATED {}", this );

            this.server.getModuleManager().notifyModuleListeners( listener -> listener.moduleUpdated( this ) );
        } );
    }

    void bundleUnresolved()
    {
        this.server.getLock().write( () -> {
            LOG.info( "UNRESOLVING {}", this );

            // for each bundle revision that no longer has a bundle wiring, but still has a corresponding module
            // revision, call its 'revisionUnresolved' method
            this.bundle.adapt( BundleRevisions.class )
                       .getRevisions()
                       .stream()
                       .filter( bundleRevision -> bundleRevision.getWiring() == null )
                       .map( this.revisions::get )
                       .filter( moduleRevision -> moduleRevision != null )
                       .forEach( ModuleRevisionImpl::revisionUnresolved );

            LOG.info( "UNRESOLVED {}", this );

            this.server.getModuleManager().notifyModuleListeners( listener -> listener.moduleUnresolved( this ) );
        } );
    }

    void bundleUninstalled()
    {
        this.server.getLock().write( () -> {
            LOG.info( "UNINSTALLED {}", this );
            this.server.getModuleManager().notifyModuleListeners( listener -> listener.moduleUninstalled( this ) );
        } );
    }
}
