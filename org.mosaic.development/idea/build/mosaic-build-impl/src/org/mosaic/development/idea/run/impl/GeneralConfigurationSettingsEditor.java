package org.mosaic.development.idea.run.impl;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.ui.RawCommandLineEditor;
import javax.swing.*;
import org.jetbrains.annotations.NotNull;
import org.mosaic.development.idea.server.MosaicServer;
import org.mosaic.development.idea.server.MosaicServerManager;

import static com.intellij.openapi.fileChooser.FileChooserDescriptorFactory.createSingleFolderDescriptor;
import static com.intellij.openapi.ui.TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT;

/**
 * @author arik
 */
public class GeneralConfigurationSettingsEditor extends SettingsEditor<MosaicRunConfiguration>
{
    private JPanel container;

    private JComboBox<MosaicServer> serverField;

    private JSpinner jmxPortField;

    private TextFieldWithBrowseButton appsLocationField;

    private TextFieldWithBrowseButton etcLocationField;

    private RawCommandLineEditor vmOptionsField;

    @SuppressWarnings("unchecked")
    public GeneralConfigurationSettingsEditor()
    {
        this.serverField.setRenderer( new ListCellRendererWrapper<MosaicServer>()
        {
            @Override
            public void customize( JList list, MosaicServer value, int index, boolean selected, boolean hasFocus )
            {
                setText( value == null ? "" : value.getName() );
            }
        } );

        this.appsLocationField.addBrowseFolderListener(
                null,
                new ComponentWithBrowseButton.BrowseFolderActionListener<>(
                        "Select applications directory",
                        "Please select the directory of your Mosaic Server application descriptors.",
                        this.appsLocationField,
                        null,
                        createSingleFolderDescriptor(),
                        TEXT_FIELD_WHOLE_TEXT )
        );
        this.etcLocationField.addBrowseFolderListener(
                null,
                new ComponentWithBrowseButton.BrowseFolderActionListener<>(
                        "Select configuration directory",
                        "Please select the directory of your Mosaic Server configurations.",
                        this.etcLocationField,
                        null,
                        createSingleFolderDescriptor(),
                        TEXT_FIELD_WHOLE_TEXT )
        );
    }

    @Override
    protected void resetEditorFrom( MosaicRunConfiguration s )
    {
        this.serverField.removeAllItems();
        MosaicServer chosenServer = null;
        for( MosaicServer server : MosaicServerManager.getInstance().getServers() )
        {
            this.serverField.addItem( server );
            if( server.getName().equals( s.getServerName() ) )
            {
                chosenServer = server;
            }
        }
        this.serverField.setSelectedItem( chosenServer );
        this.jmxPortField.setValue( s.getJmxPort() );
        this.appsLocationField.setText( s.getAppsLocation() );
        this.etcLocationField.setText( s.getEtcLocation() );
        this.vmOptionsField.setText( s.getVmOptions() );
    }

    @Override
    protected void applyEditorTo( MosaicRunConfiguration s ) throws ConfigurationException
    {
        MosaicServer server = ( MosaicServer ) this.serverField.getSelectedItem();
        s.setServerName( server != null ? server.getName() : null );
        s.setJmxPort( ( Integer ) this.jmxPortField.getValue() );
        s.setAppsLocation( this.appsLocationField.getText() );
        s.setEtcLocation( this.etcLocationField.getText() );
        s.setVmOptions( this.vmOptionsField.getText() );
    }

    @NotNull
    @Override
    protected JComponent createEditor()
    {
        return this.container;
    }
}
