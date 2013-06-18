package org.mosaic.idea.runner.task;

import com.intellij.execution.ExecutionAdapter;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.compiler.CompilationStatusAdapter;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompilerTopics;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.util.messages.MessageBusConnection;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.mosaic.idea.module.actions.BuildModulesTask;

/**
 * @author arik
 */
public class BuildMosaicModulesWhileRunning implements ProjectComponent
{
    private static final Logger LOG = Logger.getInstance( BuildMosaicModulesWhileRunning.class );

    private static final Key<List<Module>> MODULES_KEY = Key.create( BuildMosaicModulesBeforeRunTasksProvider.class.getName() + "#modules" );

    public static BuildMosaicModulesWhileRunning getInstance( Project project )
    {
        return project.getComponent( BuildMosaicModulesWhileRunning.class );
    }

    private final Project project;

    private MessageBusConnection messageBusConnection;

    private Map<Long, ExecutionEntry> modulesForExecutionIds;

    public BuildMosaicModulesWhileRunning( Project project )
    {
        this.project = project;
    }

    public void makeModulesForExecutionId( long executionId, List<Module> modules )
    {
        if( this.modulesForExecutionIds != null )
        {
            this.modulesForExecutionIds.put( executionId, new ExecutionEntry( executionId, modules ) );
        }
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
        this.modulesForExecutionIds = new ConcurrentHashMap<>( 1000 );
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
                for( Iterator<ExecutionEntry> iterator = modulesForExecutionIds.values().iterator(); iterator.hasNext(); )
                {
                    ExecutionEntry entry = iterator.next();
                    if( entry.executionId == env.getExecutionId() )
                    {
                        handler.putUserData( MODULES_KEY, entry.modules );
                        iterator.remove();
                    }
                    else if( System.currentTimeMillis() - entry.creationTime > 1000 * 60 * 5 )
                    {
                        iterator.remove();
                    }
                }

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

                    if( !modules.isEmpty() )
                    {
                        LOG.info( "Build mosaic modules after make because we have running processes monitoring these modules: " + modules );
                        ProgressManager.getInstance().run( new BuildModulesTask( project, new LinkedList<>( modules ) ) );
                    }
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
        this.modulesForExecutionIds = null;
    }

    private class ExecutionEntry
    {
        private final long creationTime = System.currentTimeMillis();

        private final long executionId;

        private final List<Module> modules;

        private ExecutionEntry( long executionId, List<Module> modules )
        {
            this.executionId = executionId;
            this.modules = modules;
        }
    }
}
