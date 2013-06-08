package org.mosaic.idea.runner.task;

import com.intellij.execution.BeforeRunTask;
import com.intellij.openapi.module.Module;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * @author arik
 */
public class BuildMosaicModulesBeforeRunTask extends BeforeRunTask<BuildMosaicModulesBeforeRunTask>
{
    @Nullable
    private List<Module> modules;

    public BuildMosaicModulesBeforeRunTask()
    {
        super( BuildMosaicModulesBeforeRunTasksProvider.ID );
    }

    @Nullable
    public List<Module> getModules()
    {
        return modules;
    }

    public void setModules( @Nullable List<Module> modules )
    {
        this.modules = modules;
    }
}
