package org.mosaic.development.idea.run.impl;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
            String name = library.getName();
            if( name == null || !name.startsWith( "Maven: org.mosaic:org.mosaic." ) )
            {
                deploymentUnits.add( DeploymentUnit.projectLibrary( this.myProject, library ) );
            }
        }

        Collections.sort( deploymentUnits, new Comparator<DeploymentUnit>()
        {
            @Override
            public int compare( DeploymentUnit o1, DeploymentUnit o2 )
            {
                if( o1 == null && o2 != null )
                {
                    return -1;
                }
                else if( o1 != null && o2 == null )
                {
                    return 1;
                }
                else if( o1 == null )
                {
                    return 0;
                }

                if( o1.getType() != o2.getType() )
                {
                    return o1.getType().compareTo( o2.getType() );
                }
                else
                {
                    return o1.getName().compareTo( o2.getName() );
                }
            }
        } );
        return deploymentUnits;
    }
}
