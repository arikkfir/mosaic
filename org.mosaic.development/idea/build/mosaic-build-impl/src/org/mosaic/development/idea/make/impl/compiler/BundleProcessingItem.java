package org.mosaic.development.idea.make.impl.compiler;

import com.intellij.openapi.compiler.FileProcessingCompiler;
import com.intellij.openapi.compiler.ValidityState;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.mosaic.development.idea.facet.OsgiBundleFacet;

import static com.intellij.util.ObjectUtils.assertNotNull;

/**
 * @author arik
 */
class BundleProcessingItem implements FileProcessingCompiler.ProcessingItem
{
    @NotNull
    private final Module module;

    @NotNull
    private final OsgiBundleFacet facet;

    BundleProcessingItem( @NotNull Module module, @NotNull OsgiBundleFacet facet )
    {
        this.module = module;
        this.facet = facet;
    }

    @NotNull
    @Override
    public VirtualFile getFile()
    {
        return assertNotNull( this.module.getModuleFile() );
    }

    @Override
    public ValidityState getValidityState()
    {
        return new BundleValidityState( this.module );
    }

    @NotNull
    public Module getModule()
    {
        return this.module;
    }

    @NotNull
    public OsgiBundleFacet getFacet()
    {
        return this.facet;
    }
}
