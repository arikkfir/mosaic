package org.mosaic.development.idea.run.impl;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.mosaic.development.idea.facet.OsgiBundleFacet;
import org.mosaic.development.idea.run.DeploymentUnit;
import org.mosaic.development.idea.run.DeploymentUnitsManager;

/**
 * @author arik
 */
public class DeploymentUnitsManagerImpl extends DeploymentUnitsManager
{
    public DeploymentUnitsManagerImpl( Project project )
    {
        super( project );
    }

    @NotNull
    public List<DeploymentUnit> getAvailableDeploymentUnits()
    {
        List<DeploymentUnit> deploymentUnits = new ArrayList<>();

        // collect module deployment units
        ModuleManager moduleManager = ModuleManager.getInstance( this.myProject );
        for( Module module : moduleManager.getSortedModules() )
        {
            OsgiBundleFacet facet = OsgiBundleFacet.getInstance( module );
            if( facet != null )
            {
                deploymentUnits.add( DeploymentUnit.module( module ) );
            }
        }

        // collect library units
        LibraryTable projectLibTable = ProjectLibraryTable.getInstance( this.myProject );
        for( Library library : projectLibTable.getLibraries() )
        {
            deploymentUnits.add( DeploymentUnit.projectLibrary( this.myProject, library ) );
        }

        return deploymentUnits;
    }
}
