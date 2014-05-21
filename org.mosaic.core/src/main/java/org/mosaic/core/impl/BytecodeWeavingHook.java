package org.mosaic.core.impl;

import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.core.util.base.ToStringHelper;
import org.mosaic.core.util.workflow.Status;
import org.mosaic.core.util.workflow.TransitionAdapter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.weaving.WeavingException;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.wiring.BundleRevision;

/**
 * @author arik
 */
class BytecodeWeavingHook extends TransitionAdapter implements WeavingHook
{
    @Nonnull
    private final ServerImpl server;

    @Nonnull
    private final BytecodeCompiler compiler;

    @Nullable
    private ServiceRegistration<WeavingHook> registration;

    BytecodeWeavingHook( @Nonnull ServerImpl server, @Nonnull BytecodeCompiler cachingCompiler )
    {
        this.server = server;
        this.compiler = cachingCompiler;
    }

    @Override
    public String toString()
    {
        return ToStringHelper.create( this )
                             .add( "compiler", this.compiler )
                             .toString();
    }

    @Override
    public void execute( @Nonnull Status origin, @Nonnull Status target ) throws Exception
    {
        if( target == ServerImpl.STARTED )
        {
            register();
        }
        else if( target == ServerImpl.STOPPED )
        {
            unregister();
        }
    }

    @Override
    public void revert( @Nonnull Status origin, @Nonnull Status target ) throws Exception
    {
        if( target == ServerImpl.STARTED )
        {
            unregister();
        }
    }

    @Override
    public void weave( @Nonnull WovenClass wovenClass )
    {
        // ignore our own bundle and specific excluded bundles
        BundleRevision bundleRevision = wovenClass.getBundleWiring().getRevision();
        Bundle bundle = bundleRevision.getBundle();
        if( bundleRevision.getBundle().getBundleId() <= 1 )
        {
            return;
        }

        // find Mosaic module for this bundle
        ModuleImpl module = this.server.getModuleManager().getModule( bundleRevision.getBundle().getBundleId() );
        if( module == null )
        {
            throw new WeavingException( "could not find module for bundle " + bundle );
        }

        // find module revision for the bundle revision this class is woven for
        ModuleRevisionImpl moduleRevision = module.getRevision( bundleRevision );
        if( moduleRevision == null )
        {
            throw new WeavingException( "could not find module revision for " + bundleRevision + " in module " + module );
        }

        // all weaved bundles should import the 'spi' package of 'modules' module
        ensureImported( wovenClass, ModulesSpi.class.getPackage().getName() );
        ensureImported( wovenClass, "javassist.runtime" );

        // joda has a bug in their manifest - we can circumvent it by importing 'org.joda.time.base'
        if( !"joda-time".equals( bundle.getSymbolicName() ) )
        {
            ensureImported( wovenClass, "org.joda.time.base" );
        }

        // compile
        byte[] bytes = this.compiler.compile( moduleRevision, wovenClass );
        if( bytes != null )
        {
            wovenClass.setBytes( bytes );
        }
    }

    private void ensureImported( @Nonnull WovenClass wovenClass, @Nonnull String... packageNames )
    {
        for( String packageName : packageNames )
        {
            if( !wovenClass.getDynamicImports().contains( packageName ) )
            {
                wovenClass.getDynamicImports().add( packageName );
            }
        }
    }

    private void register()
    {
        this.server.getLogger().trace( "Registering bytecode weaving hook" );

        BundleContext bundleContext = BytecodeWeavingHook.this.server.getBundleContext();
        this.registration = bundleContext.registerService( WeavingHook.class, BytecodeWeavingHook.this, null );
    }

    private void unregister()
    {
        ServiceRegistration<WeavingHook> registration = this.registration;
        if( registration != null )
        {
            try
            {
                this.server.getLogger().trace( "Unregistering bytecode weaving hook" );
                registration.unregister();
            }
            catch( Exception ignore )
            {
            }
            this.registration = null;
        }
    }
}
