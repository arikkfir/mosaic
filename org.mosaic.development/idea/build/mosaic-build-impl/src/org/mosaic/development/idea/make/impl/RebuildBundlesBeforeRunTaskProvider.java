package org.mosaic.development.idea.make.impl;

import com.intellij.execution.BeforeRunTaskProvider;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Key;
import com.intellij.util.concurrency.Semaphore;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mosaic.development.idea.make.BuildBundlesTaskFactory;
import org.mosaic.development.idea.run.impl.RunProfileUtil;

/**
 * @author arik
 */
public class RebuildBundlesBeforeRunTaskProvider extends BeforeRunTaskProvider<RebuildBundlesBeforeRunTask>
{
    public static final Key<RebuildBundlesBeforeRunTask> ID = Key.create( "Mosaic.BeforeRunTask" );

    @Override
    public Key<RebuildBundlesBeforeRunTask> getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return "Build OSGi Bundles";
    }

    @Override
    public String getDescription( RebuildBundlesBeforeRunTask task )
    {
        return "Builds OSGi bundles for Modules with OSGi Bundle facet.";
    }

    @Nullable
    public Icon getIcon()
    {
        return AllIcons.Javaee.WebService;
    }

    @Nullable
    @Override
    public Icon getTaskIcon( RebuildBundlesBeforeRunTask task )
    {
        return AllIcons.Javaee.WebService;
    }

    @Override
    public boolean isConfigurable()
    {
        return false;
    }

    @Nullable
    @Override
    public RebuildBundlesBeforeRunTask createTask( RunConfiguration runConfiguration )
    {
        RebuildBundlesBeforeRunTask task = new RebuildBundlesBeforeRunTask();
        task.setEnabled( true );
        return task;
    }

    @Override
    public boolean configureTask( RunConfiguration runConfiguration, RebuildBundlesBeforeRunTask task )
    {
        return false;
    }

    @Override
    public boolean canExecuteTask( RunConfiguration configuration, RebuildBundlesBeforeRunTask task )
    {
        return true;
    }

    @Override
    public boolean executeTask( DataContext context,
                                final RunConfiguration configuration,
                                ExecutionEnvironment env,
                                RebuildBundlesBeforeRunTask task )
    {
        final Project project = configuration.getProject();
        final AtomicBoolean result = new AtomicBoolean( true );
        final Semaphore semaphore = new Semaphore();
        semaphore.down();

        // get modules to build for this run configuration
        List<Module> modules = ApplicationManager.getApplication().runReadAction( new Computable<List<Module>>()
        {
            @Override
            public List<Module> compute()
            {
                return RunProfileUtil.getInstance( project ).getModulesForRunProfile( configuration );
            }
        } );

        // create the background task
        final Task.Backgroundable buildTask = BuildBundlesTaskFactory.getInstance( project ).createBuildBundlesTask(
                modules,
                new UpSemaphoe( semaphore ),
                new SetBooleanResult( result, true ),
                new SetBooleanResult( result, false ),
                new SetBooleanResult( result, false )
        );

        // run and wait for a background process that makes and builds the OSGi bundles
        ApplicationManager.getApplication().invokeAndWait( new Runnable()
        {
            @Override
            public void run()
            {
                ProgressManager.getInstance().run( buildTask );
            }
        }, ModalityState.any() );

        // wait for build to finish
        semaphore.waitFor();
        return result.get();
    }

    private class UpSemaphoe implements Runnable
    {
        @NotNull
        private final Semaphore semaphore;

        private UpSemaphoe( @NotNull Semaphore semaphore ) { this.semaphore = semaphore; }

        @Override
        public void run() { this.semaphore.up(); }
    }

    private class SetBooleanResult implements Runnable
    {
        @NotNull
        private final AtomicBoolean target;

        private final boolean value;

        private SetBooleanResult( @NotNull AtomicBoolean target, boolean value )
        {
            this.target = target;
            this.value = value;
        }

        @Override
        public void run() { this.target.set( this.value ); }
    }
}
