package org.mosaic.idea.module.actions;

import com.intellij.compiler.impl.ProjectCompileScope;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;

/**
 * @author arik
 */
public class BuildModulesAction extends AnAction
{
    @Override
    public void actionPerformed( AnActionEvent e )
    {
        Project project = e.getProject();
        if( project != null )
        {
            CompilerManager.getInstance( project ).make( new ProjectCompileScope( project ), new CompileStatusNotification()
            {
                @Override
                public void finished( boolean aborted, int errors, int warnings, CompileContext compileContext )
                {
                    Project project = compileContext.getProject();
                    if( !aborted && errors == 0 && !project.isDisposed() )
                    {
                        ProgressManager.getInstance().run( new BuildModulesTask( project ) );
                    }
                }
            } );
        }
    }
}
