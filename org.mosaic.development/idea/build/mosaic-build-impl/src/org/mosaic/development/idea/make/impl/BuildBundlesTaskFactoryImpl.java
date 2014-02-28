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
    public Task.Backgroundable createBuildBundlesTask( @NotNull List<Module> modules )
    {
        return new BuildBundlesBackgroundable( this.myProject, modules, null );
    }

    @NotNull
    @Override
    public Task.Backgroundable createBuildBundlesTask( @NotNull List<Module> modules,
                                                       Runnable onCompletion )
    {
        return new BuildBundlesBackgroundable( this.myProject, modules, onCompletion );
    }

    @NotNull
    @Override
    public Task.Backgroundable createBuildBundlesTask( @NotNull List<Module> modules,
                                                       Runnable onCompletion,
                                                       Runnable onSuccess )
    {
        return new BuildBundlesBackgroundable( this.myProject, modules, onCompletion, onSuccess, null, null );
    }

    @NotNull
    @Override
    public Task.Backgroundable createBuildBundlesTask( @NotNull List<Module> modules,
                                                       Runnable onCompletion,
                                                       Runnable onSuccess,
                                                       Runnable onFailure,
                                                       Runnable onCancel )
    {
        return new BuildBundlesBackgroundable( this.myProject, modules, onCompletion, onSuccess, onFailure, onCancel );
    }
}
