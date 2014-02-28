package org.mosaic.development.idea.run;

import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.project.Project;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * @author arik
 */
public abstract class DeploymentUnitsManager extends AbstractProjectComponent
{
    @NotNull
    public static DeploymentUnitsManager getInstance( @NotNull Project project )
    {
        return project.getComponent( DeploymentUnitsManager.class );
    }

    public DeploymentUnitsManager( Project project )
    {
        super( project );
    }

    @NotNull
    public abstract List<DeploymentUnit> getAvailableDeploymentUnits();
}
