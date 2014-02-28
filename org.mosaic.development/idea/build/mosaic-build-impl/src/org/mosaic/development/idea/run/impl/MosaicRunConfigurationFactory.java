package org.mosaic.development.idea.run.impl;

import com.intellij.compiler.options.CompileStepBeforeRun;
import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;
import org.mosaic.development.idea.make.impl.RebuildBundlesBeforeRunTaskProvider;

/**
 * @author arik
 */
public class MosaicRunConfigurationFactory extends ConfigurationFactory
{
    public MosaicRunConfigurationFactory( @NotNull ConfigurationType type )
    {
        super( type );
    }

    @Override
    public RunConfiguration createTemplateConfiguration( Project project )
    {
        return new MosaicRunConfiguration( project, this, "" );
    }

    @Override
    public boolean isConfigurationSingletonByDefault()
    {
        return true;
    }

    @Override
    public void configureBeforeRunTaskDefaults( Key<? extends BeforeRunTask> providerID, BeforeRunTask task )
    {
        super.configureBeforeRunTaskDefaults( providerID, task );
        if( providerID == CompileStepBeforeRun.ID )
        {
            task.setEnabled( true );
        }
        else if( providerID == RebuildBundlesBeforeRunTaskProvider.ID )
        {
            task.setEnabled( true );
        }
    }
}
