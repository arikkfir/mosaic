package org.mosaic.lifecycle.impl;

import javax.annotation.Nonnull;
import org.mosaic.lifecycle.Module;
import org.mosaic.lifecycle.ModuleManager;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;

import static org.osgi.framework.namespace.PackageNamespace.PACKAGE_NAMESPACE;

/**
 * @author arik
 */
public class PackageExportImpl implements Module.PackageExport
{
    @Nonnull
    private final ModuleManager moduleManager;

    @Nonnull
    private final BundleCapability capability;

    public PackageExportImpl( @Nonnull ModuleManager moduleManager, @Nonnull BundleCapability capability )
    {
        this.moduleManager = moduleManager;
        this.capability = capability;
    }

    @Nonnull
    @Override
    public Module getProvider()
    {
        Module module = this.moduleManager.getModuleFor( this.capability.getRevision() );
        if( module == null )
        {
            throw new IllegalStateException( "Could not find module for capability: " + this.capability );
        }
        else
        {
            return module;
        }
    }

    @Nonnull
    @Override
    public String getPackageName()
    {
        Object packageName = this.capability.getAttributes().get( PACKAGE_NAMESPACE );
        return packageName == null ? "unknown" : packageName.toString();
    }

    @Nonnull
    @Override
    public String getVersion()
    {
        Object version = this.capability.getAttributes().get( PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE );
        return version == null ? "0.0.0" : version.toString();
    }
}
