package org.mosaic.core.weaving.impl;

import org.mosaic.core.launcher.impl.ServerImpl;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.core.util.base.ToStringHelper;
import org.mosaic.core.weaving.WeavingSpi;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;

/**
 * @author arik
 */
public class BytecodeWeavingHook implements WeavingHook
{
    @Nonnull
    private final BytecodeCompiler compiler;

    @Nullable
    private ServiceRegistration<WeavingHook> registration;

    public BytecodeWeavingHook( @Nonnull ServerImpl server )
    {
        this.compiler = new BytecodeCachingCompiler( server );
        server.addStartupHook( bundleContext -> this.registration = bundleContext.registerService( WeavingHook.class, this, null ) );
        server.addShutdownHook( bundleContext -> {
            ServiceRegistration<WeavingHook> registration = this.registration;
            if( registration != null )
            {
                try
                {
                    registration.unregister();
                }
                catch( Exception ignore )
                {
                }
                this.registration = null;
            }
        } );
    }

    @Override
    public String toString()
    {
        return ToStringHelper.create( this ).toString();
    }

    @Override
    public void weave( @Nonnull WovenClass wovenClass )
    {
        if( wovenClass.getBundleWiring().getBundle().getBundleId() > 0 )
        {
            // all weaved bundles should import the 'spi' package of 'modules' module
            ensureImported( wovenClass, WeavingSpi.class.getPackage().getName() );
            ensureImported( wovenClass, "javassist.runtime" );

            // compile
            byte[] bytes = this.compiler.compile( wovenClass );
            if( bytes != null )
            {
                wovenClass.setBytes( bytes );
            }
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
}
