package org.mosaic.core.impl.bytecode;

import java.nio.file.Path;
import org.mosaic.core.ModuleRevision;
import org.mosaic.core.impl.ServerStatus;
import org.mosaic.core.impl.methodinterception.ModulesSpi;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.core.util.base.ToStringHelper;
import org.mosaic.core.util.workflow.Status;
import org.mosaic.core.util.workflow.TransitionAdapter;
import org.mosaic.core.util.workflow.Workflow;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.weaving.WeavingException;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.wiring.BundleRevision;
import org.slf4j.Logger;

/**
 * @author arik
 */
public class BytecodeWeavingHook extends TransitionAdapter implements WeavingHook
{
    @Nonnull
    private final Logger logger;

    @Nonnull
    private final ModuleRevisionLookup moduleRevisionLookup;

    @Nonnull
    private final BytecodeCompiler compiler;

    @Nullable
    private ServiceRegistration<WeavingHook> registration;

    public BytecodeWeavingHook( @Nonnull Workflow workflow,
                                @Nonnull Path weavingDirectory,
                                @Nonnull ModuleRevisionLookup moduleRevisionLookup )
    {
        this.logger = workflow.getLogger();
        this.moduleRevisionLookup = moduleRevisionLookup;
        this.compiler = new BytecodeCachingCompiler( workflow, weavingDirectory );
        workflow.addListener( this );
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
        if( target == ServerStatus.STARTED )
        {
            register();
        }
        else if( target == ServerStatus.STOPPED )
        {
            unregister();
        }
    }

    @Override
    public void revert( @Nonnull Status origin, @Nonnull Status target ) throws Exception
    {
        if( target == ServerStatus.STARTED )
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

        // find module revision for the bundle revision this class is woven for
        ModuleRevision moduleRevision = this.moduleRevisionLookup.getModuleRevision( bundleRevision );
        if( moduleRevision == null )
        {
            throw new WeavingException( "could not find module revision for " + bundleRevision + " in module " + bundleRevision.getBundle().getBundleId() );
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
        this.logger.trace( "Registering bytecode weaving hook" );

        Bundle bundle = FrameworkUtil.getBundle( getClass() );
        if( bundle == null )
        {
            throw new IllegalStateException();
        }

        BundleContext bundleContext = bundle.getBundleContext();
        if( bundleContext == null )
        {
            throw new IllegalStateException();
        }

        this.registration = bundleContext.registerService( WeavingHook.class, this, null );
    }

    private void unregister()
    {
        ServiceRegistration<WeavingHook> registration = this.registration;
        if( registration != null )
        {
            try
            {
                this.logger.trace( "Unregistering bytecode weaving hook" );
                registration.unregister();
            }
            catch( Exception ignore )
            {
            }
            this.registration = null;
        }
    }
}
