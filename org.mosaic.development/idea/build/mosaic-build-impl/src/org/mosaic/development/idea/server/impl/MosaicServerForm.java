package org.mosaic.development.idea.server.impl;

import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.VirtualFile;
import java.io.File;
import java.nio.charset.Charset;
import java.util.Objects;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.fileChooser.FileChooserDescriptorFactory.createSingleFolderDescriptor;
import static com.intellij.openapi.ui.TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT;
import static java.nio.file.Files.readAllBytes;

/**
 * @author arik
 */
public class MosaicServerForm
{

    private static final Charset UTF_8 = Charset.forName( "UTF-8" );

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
                        updateVersion();
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

    public void setName( String name )
    {
        this.nameField.setText( Objects.toString( name, "" ) );
    }

    @NotNull
    public String getLocation()
    {
        return this.locationField.getText();
    }

    public void setLocation( String location )
    {
        this.locationField.setText( Objects.toString( location, "" ) );
        updateVersion();
    }

    @NotNull
    public String getVersion()
    {
        return this.versionLabel.getText();
    }

    private void updateVersion()
    {
        versionLabel.setText( "Unknown" );

        String location = this.locationField.getText();
        if( location == null || location.trim().isEmpty() )
        {
            return;
        }

        File chosenFile = new File( location );
        if( chosenFile.exists() && chosenFile.isDirectory() )
        {
            File versionFile = new File( chosenFile, "version" );
            if( versionFile.exists() && !versionFile.isDirectory() )
            {
                try
                {
                    String version = new String( readAllBytes( versionFile.toPath() ), UTF_8 );
                    versionLabel.setText( version.trim().isEmpty() ? "Unknown" : version.trim() );
                }
                catch( Exception ignore )
                {
                }
            }
        }
    }
}
