package org.mosaic.idea.runner.task;

import com.intellij.execution.ExecutionAdapter;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.compiler.CompilationStatusAdapter;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompilerTopics;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBusConnection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.mosaic.idea.module.actions.BuildModulesTask;

import static org.mosaic.idea.runner.task.BuildMosaicModulesBeforeRunTasksProvider.MODULES_KEY;

/**
 * @author arik
 */
public class BuildMosaicModulesWhileRunning implements ProjectComponent
{
    private final Project project;

    private MessageBusConnection messageBusConnection;

    public BuildMosaicModulesWhileRunning( Project project )
    {
        this.project = project;
    }

    @NotNull
    @Override
    public String getComponentName()
    {
        return BuildMosaicModulesWhileRunning.class.getSimpleName();
    }

    @Override
    public void initComponent()
    {
        // no-op
    }

    @Override
    public void projectOpened()
    {
        this.messageBusConnection = this.project.getMessageBus().connect();

        this.messageBusConnection.subscribe( ExecutionManager.EXECUTION_TOPIC, new ExecutionAdapter()
        {
            @Override
            public void processStarted( String executorId,
                                        @NotNull ExecutionEnvironment env,
                                        @NotNull ProcessHandler handler )
            {
                handler.putUserData( MODULES_KEY, env.getUserData( MODULES_KEY ) );
            }
        } );

        this.messageBusConnection.subscribe( CompilerTopics.COMPILATION_STATUS, new CompilationStatusAdapter()
        {
            @Override
            public void compilationFinished( boolean aborted, int errors, int warnings, CompileContext compileContext )
            {
                if( !aborted && errors == 0 )
                {
                    Set<Module> modules = new LinkedHashSet<>();

                    ExecutionManager executionManager = ExecutionManager.getInstance( compileContext.getProject() );
                    for( ProcessHandler process : executionManager.getRunningProcesses() )
                    {
                        if( !process.isProcessTerminating() && !process.isProcessTerminated() )
                        {
                            List<Module> processModules = process.getUserData( MODULES_KEY );
                            if( processModules != null )
                            {
                                modules.addAll( processModules );
                            }
                        }
                    }

                    ProgressManager.getInstance().run( new BuildModulesTask( project, new LinkedList<>( modules ) ) );
                }
            }
        } );
    }

    @Override
    public void projectClosed()
    {
        this.messageBusConnection.disconnect();
    }

    @Override
    public void disposeComponent()
    {
        // no-op
    }
}
