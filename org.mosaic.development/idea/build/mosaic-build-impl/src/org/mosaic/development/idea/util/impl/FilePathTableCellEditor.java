package org.mosaic.development.idea.util.impl;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.AbstractTableCellEditor;
import com.intellij.util.ui.CellEditorComponentWithBrowseButton;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.Objects;
import javax.swing.JTable;
import javax.swing.JTextField;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.fileChooser.FileChooserDescriptorFactory.createSingleFolderDescriptor;

public class FilePathTableCellEditor extends AbstractTableCellEditor
{
    @NotNull
    private final CellEditorComponentWithBrowseButton<JTextField> component;

    public FilePathTableCellEditor( String value )
    {
        this.component = new CellEditorComponentWithBrowseButton<>( new TextFieldWithBrowseButton(), this );
        this.component.getChildComponent().setEditable( false );
        this.component.getChildComponent().setBorder( null );
        this.component.getChildComponent().setText( value );

        this.component.getComponentWithButton().getButton().addActionListener( new ActionListener()
        {
            @Override
            public void actionPerformed( ActionEvent e )
            {
                JTextField textField = FilePathTableCellEditor.this.component.getChildComponent();
                String path = textField.getText();
                VirtualFile value = null;
                if( path != null && !path.isEmpty() )
                {
                    URL url = VfsUtil.convertToURL( VfsUtil.pathToUrl( path ) );
                    if( url != null )
                    {
                        value = VfsUtil.findFileByURL( url );
                    }
                }

                value = FileChooser.chooseFile( createSingleFolderDescriptor(), textField, null, value );
                textField.setText( value == null ? "" : value.getPath() );
                textField.requestFocus();

                onUpdate();
            }
        } );
    }

    @Override
    public Object getCellEditorValue()
    {
        return this.component.getChildComponent().getText();
    }

    @Override
    public Component getTableCellEditorComponent( JTable table, Object value, boolean isSelected, int row, int column )
    {
        this.component.getChildComponent().setText( Objects.toString( value, "" ) );
        return this.component;
    }

    protected void onUpdate()
    {
        // no-op
    }
}
