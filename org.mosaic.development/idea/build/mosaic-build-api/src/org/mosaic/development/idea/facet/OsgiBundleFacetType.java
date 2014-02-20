package org.mosaic.development.idea.facet;

import com.intellij.facet.FacetType;
import com.intellij.facet.FacetTypeId;
import org.jetbrains.annotations.NotNull;

/**
 * @author arik
 */
public abstract class OsgiBundleFacetType extends FacetType<OsgiBundleFacet, OsgiBundleFacetConfiguration>
{
    private static final String TYPE_STRING_ID = "osgiBundleFacet";

    public static final FacetTypeId<OsgiBundleFacet> TYPE_ID = new FacetTypeId<>( TYPE_STRING_ID );

    @NotNull
    public static OsgiBundleFacetType getOsgiBundleFacetType()
    {
        return FacetType.findInstance( OsgiBundleFacetType.class );
    }

    public OsgiBundleFacetType()
    {
        super( TYPE_ID, TYPE_STRING_ID, "OSGi Bundle" );
    }
}
