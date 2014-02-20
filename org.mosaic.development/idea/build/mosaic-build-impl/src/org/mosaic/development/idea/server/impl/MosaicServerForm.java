package org.mosaic.development.idea.server.impl;

import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.fileChooser.FileChooserDescriptorFactory.createSingleFolderDescriptor;
import static com.intellij.openapi.ui.TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT;

/**
 * @author arik
 */
public class MosaicServerForm
{
    private JTextField nameField;

    private TextFieldWithBrowseButton locationField;

    private JLabel versionLabel;

    private JPanel container;

    public MosaicServerForm()
    {
        String title = "Mosaic Server location";
        String description = "Please select the home directory of your Mosaic Server installation.";
        this.locationField.addBrowseFolderListener(
                null,
                new ComponentWithBrowseButton.BrowseFolderActionListener<JTextField>( title, description, this.locationField, null, createSingleFolderDescriptor(), TEXT_FIELD_WHOLE_TEXT )
                {
                    @Override
                    protected void onFileChoosen( @NotNull VirtualFile chosenFile )
                    {
                        super.onFileChoosen( chosenFile );
                        versionLabel.setText( "Unknown" );
                        if( chosenFile.isValid() && chosenFile.exists() && chosenFile.isDirectory() )
                        {
                            VirtualFile versionFile = chosenFile.findChild( "version" );
                            if( versionFile != null && versionFile.exists() && !versionFile.isDirectory() )
                            {
                                try
                                {
                                    String version = VfsUtilCore.loadText( versionFile );
                                    versionLabel.setText( version.trim().isEmpty() ? "Unknown" : version.trim() );
                                }
                                catch( Exception ignore )
                                {
                                }
                            }
                        }
                    }
                }
        );
    }

    @NotNull
    public JPanel getContainer()
    {
        return this.container;
    }

    @NotNull
    public String getName()
    {
        return this.nameField.getText();
    }

    @NotNull
    public String getLocation()
    {
        return this.locationField.getText();
    }

    @NotNull
    public String getVersion()
    {
        return this.versionLabel.getText();
    }
}
