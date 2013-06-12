package org.mosaic.idea.runner.task;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModulePointer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.ModulesAlphaComparator;
import com.intellij.ui.SortedListModel;
import com.intellij.ui.components.JBList;
import java.awt.Dimension;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import org.jetbrains.annotations.NotNull;
import org.mosaic.idea.module.facet.ModuleFacet;

/**
 * @author arik
 */
@SuppressWarnings( "unchecked" )
public class SelectMosaicModulesForm
{
    private JBList modules;

    private JPanel panel;

    private SortedListModel<Module> model = new SortedListModel<>( ModulesAlphaComparator.INSTANCE );

    public SelectMosaicModulesForm( @NotNull Project project )
    {
        this.panel.setPreferredSize( new Dimension( 400, 300 ) );
        this.model.setAll( ModuleFacet.findMosaicModules( project ) );
        this.modules.setModel( this.model );
    }

    public JPanel getPanel()
    {
        return panel;
    }

    public JComponent getPreferredFocusComponent()
    {
        return this.modules;
    }

    public List<Module> getSelectedModules()
    {
        return this.modules.getSelectedValuesList();
    }

    public void setSelectedModules( @NotNull List<ModulePointer> modules )
    {
        ListSelectionModel selectionModel = this.modules.getSelectionModel();
        selectionModel.setValueIsAdjusting( true );
        selectionModel.clearSelection();
        for( ModulePointer modulePointer : modules )
        {
            Module module = modulePointer.getModule();
            if( module != null )
            {
                int index = this.model.indexOf( module );
                selectionModel.addSelectionInterval( index, index );
            }
        }
        selectionModel.setValueIsAdjusting( false );
    }
}
