package org.mosaic.development.idea.facet.impl;

import com.intellij.facet.Facet;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleTypeManager;
import com.intellij.openapi.util.IconLoader;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mosaic.development.idea.facet.OsgiBundleFacet;
import org.mosaic.development.idea.facet.OsgiBundleFacetConfiguration;
import org.mosaic.development.idea.facet.OsgiBundleFacetType;

/**
 * @author arik
 */
public class OsgiBundleFacetTypeImpl extends OsgiBundleFacetType
{
    private static final String JAVA__MODULE_TYPE = "JAVA_MODULE";

    @NotNull
    @Override
    public OsgiBundleFacetConfiguration createDefaultConfiguration()
    {
        return new OsgiBundleFacetConfigurationImpl();
    }

    @NotNull
    @Override
    public OsgiBundleFacet createFacet( @NotNull Module module,
                                        @NotNull String name,
                                        @NotNull OsgiBundleFacetConfiguration configuration,
                                        @Nullable Facet underlyingFacet )
    {
        return new OsgiBundleFacetImpl( this, module, name, configuration, underlyingFacet );
    }

    @NotNull
    @Override
    public Icon getIcon()
    {
        return IconLoader.getIcon( "/nodes/ppJar.png" );
    }

    @Override
    public boolean isSuitableModuleType( @NotNull ModuleType moduleType )
    {
        ModuleType javaModuleType = ModuleTypeManager.getInstance().findByID( JAVA__MODULE_TYPE );
        return javaModuleType != null && moduleType.equals( javaModuleType );
    }
}
