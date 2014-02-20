package org.mosaic.development.idea.make;

import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * @author arik
 */
public abstract class BuildBundlesTaskFactory extends AbstractProjectComponent
{
    @NotNull
    public static BuildBundlesTaskFactory getInstance( @NotNull Project project )
    {
        return project.getComponent( BuildBundlesTaskFactory.class );
    }

    protected BuildBundlesTaskFactory( Project project )
    {
        super( project );
    }

    @NotNull
    public abstract Task.Backgroundable createBuildBundlesTask( @NotNull Project project,
                                                                @NotNull List<Module> modules );

    @NotNull
    public abstract Task.Backgroundable createBuildBundlesTask( @NotNull Project project,
                                                                @NotNull List<Module> modules,
                                                                Runnable completion );
}
