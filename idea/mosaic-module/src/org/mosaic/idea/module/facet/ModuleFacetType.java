package org.mosaic.idea.module.facet;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetType;
import com.intellij.facet.FacetTypeId;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleTypeManager;
import com.intellij.openapi.util.IconLoader;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author arik
 */
public class ModuleFacetType extends FacetType<ModuleFacet, ModuleFacetConfiguration>
{
    private static final String TYPE_STRING_ID = "mosaicModuleFacet";

    private static final String JAVA__MODULE_TYPE = "JAVA_MODULE";

    public static final FacetTypeId<ModuleFacet> TYPE_ID = new FacetTypeId<>( TYPE_STRING_ID );

    public static ModuleFacetType getFacetType()
    {
        return FacetType.findInstance( ModuleFacetType.class );
    }

    public ModuleFacetType()
    {
        super( TYPE_ID, TYPE_STRING_ID, "Mosaic" );
    }

    @Override
    public ModuleFacetConfiguration createDefaultConfiguration()
    {
        return new ModuleFacetConfiguration();
    }

    @Override
    public ModuleFacet createFacet( @NotNull Module module,
                                    String name,
                                    @NotNull ModuleFacetConfiguration configuration,
                                    @Nullable Facet underlyingFacet )
    {
        return new ModuleFacet( this, module, name, configuration, underlyingFacet );
    }

    @Override
    public Icon getIcon()
    {
        return IconLoader.getIcon( "/nodes/ppJar.png" );
    }

    @Override
    public boolean isSuitableModuleType( ModuleType moduleType )
    {
        ModuleType javaModuleType = ModuleTypeManager.getInstance().findByID( JAVA__MODULE_TYPE );
        return javaModuleType != null && moduleType.equals( javaModuleType );
    }
}
