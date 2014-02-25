package org.mosaic.development.idea.run;

import com.intellij.execution.RunnerRegistry;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.impl.DefaultJavaProgramRunner;
import com.intellij.execution.runners.ProgramRunner;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * @author arik
 */
public class MosaicProgramRunner extends DefaultJavaProgramRunner
{
    @NonNls
    public static final String MOSAIC_JAVA_RUNNER_ID = "MosaicRun";

    public static ProgramRunner getInstance()
    {
        return RunnerRegistry.getInstance().findRunnerById( MOSAIC_JAVA_RUNNER_ID );
    }

    @Override
    public boolean canRun( @NotNull final String executorId, @NotNull final RunProfile profile )
    {
        return executorId.equals( DefaultRunExecutor.EXECUTOR_ID ) && profile instanceof MosaicRunConfiguration;
    }

    @Override
    @NotNull
    public String getRunnerId()
    {
        return MOSAIC_JAVA_RUNNER_ID;
    }
}
