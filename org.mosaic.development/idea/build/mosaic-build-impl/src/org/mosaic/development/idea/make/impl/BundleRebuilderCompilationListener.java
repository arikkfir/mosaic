package org.mosaic.development.idea.make.impl;

import com.intellij.execution.ExecutionAdapter;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.compiler.CompilationStatusAdapter;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompilerTopics;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.util.messages.MessageBusConnection;
import java.util.*;
import org.jetbrains.annotations.NotNull;
import org.mosaic.development.idea.facet.OsgiBundleFacet;
import org.mosaic.development.idea.make.BuildBundlesTaskFactory;
import org.mosaic.development.idea.run.impl.RunProfileUtil;

import static java.util.Arrays.asList;

/**
 * @author arik
 */
public class BundleRebuilderCompilationListener extends AbstractProjectComponent
{
    private MessageBusConnection bus;

    private Map<Module, Integer> moduleUseCount = new HashMap<>();

    public BundleRebuilderCompilationListener( Project project )
    {
        super( project );
    }

    @Override
    public void initComponent()
    {
        this.bus = this.myProject.getMessageBus().connect();

        // register a listener which tracks which modules are participating in currently running configurations
        this.bus.subscribe( ExecutionManager.EXECUTION_TOPIC, new ExecutionAdapter()
        {
            @Override
            public void processStarted( String executorId,
                                        @NotNull ExecutionEnvironment env,
                                        @NotNull ProcessHandler handler )
            {
                RunProfileUtil util = RunProfileUtil.getInstance( BundleRebuilderCompilationListener.this.myProject );
                for( Module module : getModulesToBuild( util.getModulesForRunProfile( env.getRunProfile() ) ) )
                {
                    incrementModuleUseCount( module );
                }
            }

            @Override
            public void processTerminated( @NotNull RunProfile runProfile, @NotNull ProcessHandler handler )
            {
                RunProfileUtil util = RunProfileUtil.getInstance( BundleRebuilderCompilationListener.this.myProject );
                for( Module module : getModulesToBuild( util.getModulesForRunProfile( runProfile ) ) )
                {
                    decrementModuleUseCount( module );
                }
            }
        } );

        // register a listener which will rebuild bundles participating in any running configuration
        this.bus.subscribe( CompilerTopics.COMPILATION_STATUS, new CompilationStatusAdapter()
        {
            @Override
            public void compilationFinished( boolean aborted,
                                             int errors,
                                             int warnings,
                                             final CompileContext compileContext )
            {
                // only operate when compilation has a context
                Project project = compileContext.getProject();
                CompileScope compileScope = compileContext.getCompileScope();
                if( !project.isDisposed() && compileScope != null )
                {
                    // get the modules that are currently deployed
                    final List<Module> modules = getModulesToBuild( compileScope.getAffectedModules() );
                    Iterator<Module> iterator = modules.iterator();
                    while( iterator.hasNext() )
                    {
                        Module module = iterator.next();
                        int useCount = getModuleUseCount( module );
                        if( useCount <= 0 )
                        {
                            iterator.remove();
                        }
                    }

                    if( !modules.isEmpty() )
                    {
                        // rebuild bundles
                        if( ApplicationManager.getApplication().isDispatchThread() )
                        {
                            runBackgroundTask( modules );
                        }
                        else
                        {
                            ApplicationManager.getApplication().invokeAndWait( new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    runBackgroundTask( modules );
                                }
                            }, ModalityState.any() );
                        }
                    }
                }
            }
        } );
    }

    @Override
    public void disposeComponent()
    {
        this.bus.disconnect();
        this.bus = null;
    }

    private synchronized int getModuleUseCount( Module module )
    {
        return this.moduleUseCount.containsKey( module ) ? this.moduleUseCount.get( module ) : 0;
    }

    private synchronized void incrementModuleUseCount( Module module )
    {
        Integer useCount = this.moduleUseCount.get( module );
        this.moduleUseCount.put( module, useCount == null ? 1 : useCount + 1 );
    }

    private synchronized void decrementModuleUseCount( Module module )
    {
        Integer useCount = this.moduleUseCount.get( module );
        this.moduleUseCount.put( module, useCount == null ? 0 : useCount - 1 );
    }

    private void runBackgroundTask( final List<Module> modules )
    {
        BuildBundlesTaskFactory buildBundlesTaskFactory = BuildBundlesTaskFactory.getInstance( this.myProject );
        ProgressManager.getInstance().run( buildBundlesTaskFactory.createBuildBundlesTask( modules ) );
    }

    @NotNull
    private List<Module> getModulesToBuild( final Module[] deployedModules )
    {
        return getModulesToBuild( asList( deployedModules ) );
    }

    @NotNull
    private List<Module> getModulesToBuild( final List<Module> deployedModules )
    {
        // collect all modules for deployment, and their dependencies, into the module set
        List<Module> modulesToBuild = ApplicationManager.getApplication().runReadAction( new Computable<List<Module>>()
        {
            @Override
            public List<Module> compute()
            {
                ModuleManager moduleManager = ModuleManager.getInstance( BundleRebuilderCompilationListener.this.myProject );
                Set<Module> sortedModules = new TreeSet<>( moduleManager.moduleDependencyComparator() );
                for( Module deployedModule : deployedModules )
                {
                    ModuleUtil.getDependencies( deployedModule, sortedModules );
                }
                return new LinkedList<>( sortedModules );
            }
        } );

        // remove any modules which are not mavenized or not bundlized
        Iterator<Module> modulesToBuildIterator = modulesToBuild.iterator();
        while( modulesToBuildIterator.hasNext() )
        {
            Module module = modulesToBuildIterator.next();
            if( OsgiBundleFacet.getInstance( module ) == null )
            {
                modulesToBuildIterator.remove();
            }
        }
        return modulesToBuild;
    }
}
