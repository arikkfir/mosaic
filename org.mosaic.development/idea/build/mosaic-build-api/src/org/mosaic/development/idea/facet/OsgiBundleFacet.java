package org.mosaic.development.idea.facet;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetType;
import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author arik
 */
public abstract class OsgiBundleFacet extends Facet<OsgiBundleFacetConfiguration>
{
    @Nullable
    public static OsgiBundleFacet getInstance( @NotNull Module module )
    {
        return FacetManager.getInstance( module ).getFacetByType( OsgiBundleFacetType.TYPE_ID );
    }

    protected OsgiBundleFacet( @NotNull FacetType facetType,
                               @NotNull Module module,
                               @NotNull String name,
                               @NotNull OsgiBundleFacetConfiguration configuration,
                               Facet underlyingFacet )
    {
        super( facetType, module, name, configuration, underlyingFacet );
    }
}
