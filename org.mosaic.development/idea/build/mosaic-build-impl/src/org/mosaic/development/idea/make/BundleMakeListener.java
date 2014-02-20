package org.mosaic.development.idea.make;

import com.intellij.openapi.compiler.CompilationStatusAdapter;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

import static java.util.Arrays.asList;

/**
 * @author arik
 */
public class BundleMakeListener extends AbstractProjectComponent
{
    @NotNull
    private final BundleMakeListener.MyCompilationStatusAdapter compilationStatusAdapter = new MyCompilationStatusAdapter();

    public BundleMakeListener( Project project )
    {
        super( project );
    }

    @Override
    public void projectOpened()
    {
        CompilerManager.getInstance( this.myProject ).addCompilationStatusListener( this.compilationStatusAdapter );
    }

    @Override
    public void projectClosed()
    {
        CompilerManager.getInstance( this.myProject ).removeCompilationStatusListener( this.compilationStatusAdapter );
    }

    private class MyCompilationStatusAdapter extends CompilationStatusAdapter
    {
        @Override
        public void compilationFinished( boolean aborted,
                                         int errors,
                                         int warnings,
                                         @NotNull CompileContext compileContext )
        {
            if( !aborted && errors <= 0 )
            {
                // get list of compiled modules, and sorted list of all modules (depending before dependant in sorted list)
                List<Module> affectedModules = asList( compileContext.getCompileScope().getAffectedModules() );
                List<Module> modulesToBuild = new LinkedList<>( asList( ModuleManager.getInstance( myProject ).getSortedModules() ) );

                // keep only modules that were compiled, but in a sorted way
                Iterator<Module> iterator = modulesToBuild.iterator();
                while( iterator.hasNext() )
                {
                    Module module = iterator.next();
                    if( !affectedModules.contains( module ) )
                    {
                        iterator.remove();
                    }
                }

                // build in background
                Project project = compileContext.getProject();
                // TODO: ProgressManager.getInstance().run( new BuildBundlesBackgroundable( project, modulesToBuild, completion ) );
            }
        }
    }
}
