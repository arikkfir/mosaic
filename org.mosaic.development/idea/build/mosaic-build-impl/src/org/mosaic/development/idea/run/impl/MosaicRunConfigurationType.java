package org.mosaic.development.idea.run.impl;

import com.intellij.execution.configurations.ConfigurationTypeBase;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.Nullable;

/**
 * @author arik
 */
public class MosaicRunConfigurationType extends ConfigurationTypeBase
{
    @Nullable
    public static MosaicRunConfigurationType getInstance()
    {
        return ContainerUtil.findInstance( Extensions.getExtensions( CONFIGURATION_TYPE_EP ), MosaicRunConfigurationType.class );
    }

    public MosaicRunConfigurationType()
    {
        super( "Mosaic", "Mosaic Server", "Run a Mosaic Server instance", AllIcons.Javaee.WebService );
        addFactory( new MosaicRunConfigurationFactory( this ) );
    }
}
