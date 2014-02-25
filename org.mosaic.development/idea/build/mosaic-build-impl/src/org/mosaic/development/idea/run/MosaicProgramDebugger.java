package org.mosaic.development.idea.run;

import com.intellij.debugger.impl.GenericDebuggerRunner;
import com.intellij.execution.RunnerRegistry;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.runners.ProgramRunner;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * @author arik
 */
public class MosaicProgramDebugger extends GenericDebuggerRunner
{
    @NonNls
    public static final String MOSAIC_JAVA_DEBUGGER_ID = "MosaicDebug";

    public static ProgramRunner getInstance()
    {
        return RunnerRegistry.getInstance().findRunnerById( MOSAIC_JAVA_DEBUGGER_ID );
    }

    @Override
    public boolean canRun( @NotNull final String executorId, @NotNull final RunProfile profile )
    {
        return executorId.equals( DefaultDebugExecutor.EXECUTOR_ID ) && profile instanceof MosaicRunConfiguration;
    }

    @Override
    @NotNull
    public String getRunnerId()
    {
        return MOSAIC_JAVA_DEBUGGER_ID;
    }
}
