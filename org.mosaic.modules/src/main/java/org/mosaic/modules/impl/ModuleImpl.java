package org.mosaic.modules.impl;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import javax.annotation.Nonnull;
import org.joda.time.DateTime;
import org.mosaic.modules.*;
import org.mosaic.util.collections.LinkedHashMapEx;
import org.mosaic.util.collections.MapEx;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
final class ModuleImpl extends Lifecycle implements Module
{
    private static final Logger LOG = LoggerFactory.getLogger( ModuleImpl.class );

    private static final Logger MODULE_INSTALL_LOG = LoggerFactory.getLogger( Module.class.getName() + ".installed" );

    private static final Logger MODULE_RESOLVE_LOG = LoggerFactory.getLogger( Module.class.getName() + ".resolved" );

    private static final Logger MODULE_STARTING_LOG = LoggerFactory.getLogger( Module.class.getName() + ".starting" );

    private static final Logger MODULE_STARTED_LOG = LoggerFactory.getLogger( Module.class.getName() + ".started" );

    private static final Logger MODULE_ACTIVATING_LOG = LoggerFactory.getLogger( Module.class.getName() + ".activating" );

    private static final Logger MODULE_ACTIVATED_LOG = LoggerFactory.getLogger( Module.class.getName() + ".activated" );

    private static final Logger MODULE_DEACTIVATING_LOG = LoggerFactory.getLogger( Module.class.getName() + ".deactivating" );

    private static final Logger MODULE_DEACTIVATED_LOG = LoggerFactory.getLogger( Module.class.getName() + ".deactivated" );

    private static final Logger MODULE_STOPPING_LOG = LoggerFactory.getLogger( Module.class.getName() + ".stopping" );

    private static final Logger MODULE_STOPPED_LOG = LoggerFactory.getLogger( Module.class.getName() + ".stopped" );

    private static final Logger MODULE_UPDATED_LOG = LoggerFactory.getLogger( Module.class.getName() + ".updated" );

    private static final Logger MODULE_UNRESOLVED_LOG = LoggerFactory.getLogger( Module.class.getName() + ".unresolved" );

    private static final Logger MODULE_UNINSTALLED_LOG = LoggerFactory.getLogger( Module.class.getName() + ".uninstalled" );

    @Nonnull
    private final ModuleManagerImpl moduleManager;

    @Nonnull
    private final Bundle bundle;

    private final boolean internal;

    ModuleImpl( @Nonnull ModuleManagerImpl moduleManager, @Nonnull Bundle bundle, boolean internal )
    {
        this.moduleManager = moduleManager;
        this.bundle = bundle;
        this.internal = internal;

        addChild( new ModuleResourcesImpl( this ) );
        addChild( new ModuleTypesImpl( this ) );
        addChild( new ModuleWiringImpl( this ) );
    }

    @Nonnull
    @Override
    public ModuleContext getContext()
    {
        return this.moduleManager;
    }

    @Override
    public long getId()
    {
        return this.bundle.getBundleId();
    }

    @Nonnull
    @Override
    public String getName()
    {
        return this.bundle.getSymbolicName();
    }

    @Nonnull
    @Override
    public Version getVersion()
    {
        return new Version( this.bundle.getVersion().toString() );
    }

    @Nonnull
    @Override
    public Path getPath()
    {
        return Paths.get( this.bundle.getLocation() );
    }

    @Nonnull
    @Override
    public MapEx<String, String> getHeaders()
    {
        LinkedHashMapEx<String, String> ex = new LinkedHashMapEx<>();

        Dictionary<String, String> headers = this.bundle.getHeaders();
        Enumeration<String> keys = headers.keys();
        while( keys.hasMoreElements() )
        {
            String headerName = keys.nextElement();
            ex.put( headerName, headers.get( headerName ) );
        }

        return ex;
    }

    @Nonnull
    @Override
    public DateTime getLastModified()
    {
        return new DateTime( this.bundle.getLastModified() );
    }

    @Nonnull
    @Override
    public ModuleState getState()
    {
        switch( this.bundle.getState() )
        {
            case Bundle.INSTALLED:
                return ModuleState.INSTALLED;
            case Bundle.RESOLVED:
                return ModuleState.RESOLVED;
            case Bundle.STARTING:
                return ModuleState.STARTING;
            case Bundle.ACTIVE:
                return isActivated() ? ModuleState.ACTIVE : ModuleState.STARTED;
            case Bundle.STOPPING:
                return isActivated() ? ModuleState.ACTIVE : ModuleState.STOPPING;
            case Bundle.UNINSTALLED:
                return ModuleState.UNINSTALLED;
            default:
                throw new IllegalStateException( "unknown OSGi bundle state: " + this.bundle.getState() );
        }
    }

    @Nonnull
    @Override
    public ModuleResourcesImpl getModuleResources()
    {
        return requireChild( ModuleResourcesImpl.class );
    }

    @Nonnull
    @Override
    public ModuleTypesImpl getModuleTypes()
    {
        return requireChild( ModuleTypesImpl.class );
    }

    @Nonnull
    @Override
    public ModuleWiringImpl getModuleWiring()
    {
        return requireChild( ModuleWiringImpl.class );
    }

    @Override
    public void startModule() throws ModuleStartException
    {
        try
        {
            this.bundle.start();
        }
        catch( Exception e )
        {
            throw new ModuleStartException( e, this );
        }
    }

    @Override
    public void stopModule() throws ModuleStopException
    {
        try
        {
            this.bundle.stop();
        }
        catch( Exception e )
        {
            throw new ModuleStopException( e, this );
        }
    }

    @Override
    public String toString()
    {
        return getName() + "@" + getVersion() + "[" + getId() + "]";
    }

    @Nonnull
    ModuleManagerImpl getModuleManager()
    {
        return this.moduleManager;
    }

    @Nonnull
    Bundle getBundle()
    {
        return this.bundle;
    }

    boolean isInternal()
    {
        return this.internal;
    }

    @Override
    protected synchronized void onBeforeStart()
    {
        MODULE_STARTING_LOG.info( "STARTING {}", this );
        postModuleEvent( ModuleEventType.STARTING );
    }

    @Override
    protected synchronized void onAfterStart()
    {
        MODULE_STARTED_LOG.info( "STARTED {}", this );
        postModuleEvent( ModuleEventType.STARTED );
    }

    @Override
    protected synchronized void onBeforeStop()
    {
        MODULE_STOPPING_LOG.info( "STOPPING {}", this );
        postModuleEvent( ModuleEventType.STOPPING );
    }

    @Override
    protected synchronized void onAfterStop()
    {
        MODULE_STOPPED_LOG.info( "STOPPED {}", this );
        postModuleEvent( ModuleEventType.STOPPED );
    }

    @Override
    protected synchronized void onBeforeActivate()
    {
        MODULE_ACTIVATING_LOG.info( "ACTIVATING {}", this );
        postModuleEvent( ModuleEventType.ACTIVATING );
    }

    @Override
    protected synchronized void onAfterActivate()
    {
        MODULE_ACTIVATED_LOG.info( "ACTIVATED {}", this );
        postModuleEvent( ModuleEventType.ACTIVATED );
    }

    @Override
    protected synchronized void onBeforeDeactivate()
    {
        MODULE_DEACTIVATING_LOG.info( "DEACTIVATING {}", this );
        postModuleEvent( ModuleEventType.DEACTIVATING );
    }

    @Override
    protected synchronized void onAfterDeactivate()
    {
        MODULE_DEACTIVATED_LOG.info( "DEACTIVATED {}", this );
        postModuleEvent( ModuleEventType.DEACTIVATED );
    }

    synchronized void onBundleInstalled()
    {
        MODULE_INSTALL_LOG.info( "INSTALLED {}", this );
        postModuleEvent( ModuleEventType.INSTALLED );
    }

    synchronized void onBundleResolved()
    {
        getModuleResources().clearCache();
        MODULE_RESOLVE_LOG.info( "RESOLVED {}", this );
        postModuleEvent( ModuleEventType.RESOLVED );
    }

    synchronized void onBundleStarting()
    {
        try
        {
            start();
        }
        catch( Exception e )
        {
            LOG.error( "Cannot start {}: {}", this, e.getMessage(), e );
        }
    }

    synchronized void onBundleStarted()
    {
        try
        {
            activate();
        }
        catch( Exception e )
        {
            LOG.error( "Cannot activate {}: {}", this, e.getMessage(), e );
        }
    }

    synchronized void onBundleStopping()
    {
        deactivate();
        stop();
    }

    synchronized void onBundleStopped()
    {
    }

    synchronized void onBundleUpdated()
    {
        MODULE_UPDATED_LOG.info( "UPDATED {}", this );
        postModuleEvent( ModuleEventType.UPDATED );
    }

    synchronized void onBundleUnresolved()
    {
        getModuleResources().clearCache();
        MODULE_UNRESOLVED_LOG.info( "UNRESOLVED {}", this );
        postModuleEvent( ModuleEventType.UNRESOLVED );
    }

    synchronized void onBundleUninstalled()
    {
        MODULE_UNINSTALLED_LOG.info( "UNINSTALLED {}", this );
        postModuleEvent( ModuleEventType.UNINSTALLED );
    }

    private void postModuleEvent( @Nonnull ModuleEventType eventType )
    {
        Bundle modulesBundle = FrameworkUtil.getBundle( getClass() );
        if( modulesBundle == null )
        {
            throw new IllegalStateException();
        }

        BundleContext modulesBundleContext = modulesBundle.getBundleContext();
        if( modulesBundleContext == null )
        {
            throw new IllegalStateException();
        }

        ServiceReference<EventAdmin> eventAdminReference = modulesBundleContext.getServiceReference( EventAdmin.class );
        if( eventAdminReference == null )
        {
            LOG.warn( "Event admin service not found - cannot post module event {} to listeners", eventType );
            return;
        }

        EventAdmin eventAdmin = modulesBundleContext.getService( eventAdminReference );
        try
        {
            Dictionary<String, Object> dict = new Hashtable<>();
            dict.put( "mosaicEvent", new ModuleEvent( this, eventType ) );
            eventAdmin.postEvent( new Event( "org/mosaic/modules/ModuleEvent", dict ) );
        }
        catch( Throwable e )
        {
            LOG.warn( "Posting of module event {} failed: {}", eventType, e.getMessage(), e );
        }
        finally
        {
            modulesBundleContext.ungetService( eventAdminReference );
        }
    }
}
