package org.mosaic.development.idea.make.impl;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.mosaic.development.idea.make.BuildBundlesTaskFactory;

/**
 * @author arik
 */
public class BuildBundlesTaskFactoryImpl extends BuildBundlesTaskFactory
{
    public BuildBundlesTaskFactoryImpl( Project project )
    {
        super( project );
    }

    @NotNull
    @Override
    public Task.Backgroundable createBuildBundlesTask( @NotNull Project project, @NotNull List<Module> modules )
    {
        return new BuildBundlesBackgroundable( project, modules, null );
    }

    @NotNull
    @Override
    public Task.Backgroundable createBuildBundlesTask( @NotNull Project project,
                                                       @NotNull List<Module> modules,
                                                       Runnable completion )
    {
        return new BuildBundlesBackgroundable( project, modules, completion );
    }
}
