package org.mosaic.idea.runner.task;

import com.intellij.execution.BeforeRunTaskProvider;
import com.intellij.execution.configurations.ModuleRunProfile;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.Key;
import java.util.List;
import javax.swing.Icon;
import org.jetbrains.annotations.Nullable;
import org.mosaic.idea.module.actions.BuildModulesTask;
import org.mosaic.idea.module.facet.ModuleFacet;

import static com.intellij.openapi.actionSystem.PlatformDataKeys.PROJECT;
import static java.util.Arrays.asList;
import static org.mosaic.idea.module.facet.ModuleFacet.getBuildableModules;

/**
 * @author arik
 */
public class BuildMosaicModulesBeforeRunTasksProvider extends BeforeRunTaskProvider<BuildMosaicModulesBeforeRunTask>
{
    public static final String NAME = "Build Mosaic Modules";

    public static final String DESCRIPTION = NAME;

    public static final Key<BuildMosaicModulesBeforeRunTask> ID = Key.create( "Mosaic.BuildModulesBeforeRunTask" );

    @Override
    public Key<BuildMosaicModulesBeforeRunTask> getId()
    {
        return ID;
    }

    public String getName()
    {
        return NAME;
    }

    public String getDescription( BuildMosaicModulesBeforeRunTask buildMosaicModulesBeforeRunTask )
    {
        return DESCRIPTION;
    }

    @Nullable
    public Icon getIcon()
    {
        return IconLoader.getIcon( "/com/infolinks/rinku/idea/plugin/icons/rinku.png" );
    }

    @Nullable
    @Override
    public Icon getTaskIcon( BuildMosaicModulesBeforeRunTask task )
    {
        return IconLoader.getIcon( "/com/infolinks/rinku/idea/plugin/icons/rinku.png" );
    }

    public boolean isConfigurable()
    {
        // TODO arik: allow configuring task to select which modules to build
        return false;
    }

    public boolean canExecuteTask( RunConfiguration runConfiguration,
                                   BuildMosaicModulesBeforeRunTask buildMosaicModulesBeforeRunTask )
    {
        return true;
    }

    @Nullable
    @Override
    public BuildMosaicModulesBeforeRunTask createTask( RunConfiguration runConfiguration )
    {
        BuildMosaicModulesBeforeRunTask task = new BuildMosaicModulesBeforeRunTask();
        task.setEnabled( true );
        return task;
    }

    @Override
    public boolean configureTask( RunConfiguration runConfiguration, BuildMosaicModulesBeforeRunTask task )
    {
        // TODO arik: allow user to choose which modules are built by this task
        return false;
    }

    public boolean executeTask( DataContext dataContext,
                                final RunConfiguration runConfiguration,
                                ExecutionEnvironment executionEnvironment,
                                BuildMosaicModulesBeforeRunTask buildMosaicModulesBeforeRunTask )
    {
        Project project = PROJECT.getData( dataContext );
        if( project == null )
        {
            return true;
        }

        List<ModuleFacet> moduleFacets;

        List<Module> modules = buildMosaicModulesBeforeRunTask.getModules();
        if( modules == null )
        {
            if( runConfiguration instanceof ModuleRunProfile )
            {
                ModuleRunProfile moduleRunProfile = ( ModuleRunProfile ) runConfiguration;
                moduleFacets = getBuildableModules( asList( moduleRunProfile.getModules() ) );
            }
            else
            {
                moduleFacets = getBuildableModules( project );
            }
        }
        else
        {
            moduleFacets = ModuleFacet.getBuildableModules( modules );
        }

        final BuildModulesTask task = new BuildModulesTask( project, moduleFacets );
        ApplicationManager.getApplication().invokeAndWait( new Runnable()
        {
            @Override
            public void run()
            {
                ProgressManager.getInstance().run( task.asModal() );
            }
        }, ModalityState.any() );
        return task.isSuccessful();
    }
}
