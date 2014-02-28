package org.mosaic.development.idea.run.impl;

import com.intellij.execution.configurations.ModuleRunProfile;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import java.util.List;
import org.jetbrains.annotations.NotNull;

import static java.util.Arrays.asList;

/**
 * @author arik
 */
public class RunProfileUtil extends AbstractProjectComponent
{
    @NotNull
    public static RunProfileUtil getInstance( @NotNull Project project )
    {
        return project.getComponent( RunProfileUtil.class );
    }

    public RunProfileUtil( Project project )
    {
        super( project );
    }

    @NotNull
    public List<Module> getModulesForRunProfile( @NotNull final RunProfile runProfile )
    {
        return ApplicationManager.getApplication().runReadAction( new Computable<List<Module>>()
        {
            @Override
            public List<Module> compute()
            {
                if( runProfile instanceof MosaicRunConfiguration )
                {
                    MosaicRunConfiguration mosaicRunConfiguration = ( MosaicRunConfiguration ) runProfile;
                    return asList( mosaicRunConfiguration.getModules() );
                }
                else if( runProfile instanceof ModuleRunProfile )
                {
                    ModuleRunProfile moduleRunProfile = ( ModuleRunProfile ) runProfile;
                    return asList( moduleRunProfile.getModules() );
                }
                else
                {
                    ModuleManager moduleManager = ModuleManager.getInstance( RunProfileUtil.this.myProject );
                    return asList( moduleManager.getModules() );
                }
            }
        } );
    }
}
