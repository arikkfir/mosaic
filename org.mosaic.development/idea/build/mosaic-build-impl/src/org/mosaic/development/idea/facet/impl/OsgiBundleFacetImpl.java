package org.mosaic.development.idea.facet.impl;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetType;
import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;
import org.mosaic.development.idea.facet.OsgiBundleFacet;
import org.mosaic.development.idea.facet.OsgiBundleFacetConfiguration;

/**
 * @author arik
 */
public class OsgiBundleFacetImpl extends OsgiBundleFacet
{
    public OsgiBundleFacetImpl( @NotNull FacetType facetType,
                                @NotNull Module module,
                                @NotNull String name,
                                @NotNull OsgiBundleFacetConfiguration configuration,
                                Facet underlyingFacet )
    {
        super( facetType, module, name, configuration, underlyingFacet );
    }
}
