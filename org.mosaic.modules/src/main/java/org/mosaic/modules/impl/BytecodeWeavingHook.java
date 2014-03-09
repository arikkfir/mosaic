package org.mosaic.modules.impl;

import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.Set;
import javax.annotation.Nonnull;
import org.mosaic.modules.spi.ModulesSpi;
import org.osgi.framework.Bundle;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;

/**
 * @author arik
 */
class BytecodeWeavingHook implements WeavingHook
{
    @Nonnull
    private static final Set<String> EXCLUDED_BUNDLES = Collections.unmodifiableSet( Sets.newHashSet(
            "com.google.guava",
            "org.apache.commons.lang3",
            "org.mosaic.utils.reflection",
            "org.mosaic.modules"
    ) );

    @Nonnull
    private final BytecodeCompiler compiler = new BytecodeCachingCompiler( new BytecodeJavassistCompiler() );

    @Override
    public void weave( WovenClass wovenClass )
    {
        // ignore our own bundle and specific excluded bundles
        Bundle bundle = wovenClass.getBundleWiring().getRevision().getBundle();
        if( EXCLUDED_BUNDLES.contains( bundle.getSymbolicName().toLowerCase() ) )
        {
            return;
        }

        // all weaved bundles should import the 'spi' package of 'modules' module
        ensureImported( wovenClass, ModulesSpi.class.getPackage().getName() );

        // joda has a bug in their manifest - we can circumvent it by importing 'org.joda.time.base'
        if( !"joda-time".equals( bundle.getSymbolicName() ) )
        {
            ensureImported( wovenClass, "org.joda.time.base" );
        }

        // compile
        byte[] bytes = this.compiler.compile( wovenClass );
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
}
