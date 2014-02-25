package org.mosaic.development.idea.run;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.ui.CheckBoxList;
import com.intellij.util.Function;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.jetbrains.annotations.NotNull;

/**
 * @author arik
 */
public class DeploymentSettingsEditor extends SettingsEditor<MosaicRunConfiguration>
{
    @NotNull
    private final Project project;

    private JPanel container;

    private CheckBoxList<DeploymentUnit> unitsList;

    public DeploymentSettingsEditor( @NotNull Project project )
    {
        this.project = project;
    }

    @Override
    protected void resetEditorFrom( MosaicRunConfiguration s )
    {
        DeploymentUnitsManager unitsManager = DeploymentUnitsManager.getInstance( this.project );
        this.unitsList.setItems( unitsManager.getAvailableDeploymentUnits(),
                                 new Function<DeploymentUnit, String>()
                                 {
                                     @Override
                                     public String fun( DeploymentUnit deploymentUnit )
                                     {
                                         return deploymentUnit.getName();
                                     }
                                 } );

        DeploymentUnit[] deploymentUnits = s.getDeploymentUnits();
        if( deploymentUnits != null )
        {
            for( DeploymentUnit unit : deploymentUnits )
            {
                this.unitsList.setItemSelected( unit, true );
            }
        }

    }

    @Override
    protected void applyEditorTo( MosaicRunConfiguration s ) throws ConfigurationException
    {
        List<DeploymentUnit> units = new LinkedList<>();
        int itemsCount = this.unitsList.getItemsCount();
        for( int i = 0; i < itemsCount; i++ )
        {
            if( this.unitsList.isItemSelected( i ) )
            {
                units.add( ( DeploymentUnit ) this.unitsList.getItemAt( i ) );
            }
        }
        s.setDeploymentUnits( units.toArray( new DeploymentUnit[ units.size() ] ) );
    }

    @NotNull
    @Override
    protected JComponent createEditor()
    {
        return this.container;
    }
}
