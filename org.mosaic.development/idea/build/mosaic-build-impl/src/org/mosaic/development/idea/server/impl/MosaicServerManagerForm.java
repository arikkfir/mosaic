package org.mosaic.development.idea.server.impl;

import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.util.NotNullFunction;
import java.awt.BorderLayout;
import java.io.File;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mosaic.development.idea.server.MosaicServer;

/**
 * @author arik
 */
public class MosaicServerManagerForm
{
    public static class Server
    {
        public String name;

        public String location;

        public Server( String name, String location )
        {
            this.name = name;
            this.location = location;
        }
    }

    private JPanel container;

    private JPanel listContainer;

    private DefaultListModel<Server> serversModel;

    private JBList serversList;

    public MosaicServerManagerForm()
    {
        this.serversModel = new DefaultListModel<>();
        this.serversList = new JBList( this.serversModel );
        this.serversList.installCellRenderer( new NotNullFunction<Object, JComponent>()
        {
            private final JBLabel label = new JBLabel();

            @NotNull
            @Override
            public JComponent fun( Object dom )
            {
                Server server = ( Server ) dom;
                this.label.setText( server.name + " (" + server.location + ")" );
                return this.label;
            }
        } );
        this.listContainer.add( createServersListPanel(), BorderLayout.CENTER );
    }

    @NotNull
    public JPanel getContainer()
    {
        return this.container;
    }

    public boolean isModified( @NotNull Map<String, ? extends MosaicServer> managedServers )
    {
        Map<String, String> myServers = getServers();
        for( Map.Entry<String, ? extends MosaicServer> entry : managedServers.entrySet() )
        {
            String managedServerName = entry.getKey();
            String myServerLocation = myServers.get( managedServerName );
            if( !myServers.containsKey( managedServerName ) || myServerLocation == null )
            {
                // a server was deleted
                return true;
            }
            else if( !myServerLocation.equals( entry.getValue().getLocation() ) )
            {
                // a server was modified
                return true;
            }
        }
        for( Map.Entry<String, String> entry : myServers.entrySet() )
        {
            if( !managedServers.containsKey( entry.getKey() ) )
            {
                // a server was added
                return true;
            }
        }
        return false;
    }

    @NotNull
    public Map<String, String> getServers()
    {
        Map<String, String> servers = new LinkedHashMap<>();

        Enumeration<Server> myServers = this.serversModel.elements();
        while( myServers.hasMoreElements() )
        {
            Server server = myServers.nextElement();
            servers.put( server.name, server.location );
        }
        return servers;
    }

    public void setServers( @NotNull Map<String, ? extends MosaicServer> servers )
    {
        this.serversModel.clear();
        for( MosaicServer server : servers.values() )
        {
            this.serversModel.addElement( new Server( server.getName(), server.getLocation() ) );
        }
    }

    @NotNull
    private JPanel createServersListPanel()
    {
        ToolbarDecorator decorator = ToolbarDecorator.createDecorator( this.serversList );
        decorator.setAddAction( new AnActionButtonRunnable()
        {
            @Override
            public void run( AnActionButton anActionButton )
            {
                doAdd();
            }
        } );
        decorator.setEditAction( new AnActionButtonRunnable()
        {
            @Override
            public void run( AnActionButton anActionButton )
            {
                doEdit();
            }
        } );
        decorator.setRemoveAction( new AnActionButtonRunnable()
        {
            @Override
            public void run( AnActionButton anActionButton )
            {
                doRemove();
            }
        } );
        decorator.setToolbarPosition( ActionToolbarPosition.RIGHT );
        return decorator.disableUpDownActions().createPanel();
    }

    private void doAdd()
    {
        MosaicServerDialog dlg = new MosaicServerDialog()
        {
            @Override
            protected void doOKAction( @NotNull MosaicServerForm form )
            {
                String name = form.getName();
                String location = form.getLocation();
                MosaicServerManagerForm.this.serversModel.addElement( new Server( name, location ) );
            }
        };
        dlg.setModal( true );
        dlg.setTitle( "Add Mosaic Server" );
        dlg.show();
    }

    private void doEdit()
    {
        final int selectedIndex = this.serversList.getSelectedIndex();
        if( selectedIndex < 0 )
        {
            return;
        }

        final Server server = this.serversModel.elementAt( selectedIndex );
        if( server == null )
        {
            return;
        }

        MosaicServerDialog dlg = new MosaicServerDialog(server)
        {
            @Override
            protected void doOKAction( @NotNull MosaicServerForm form )
            {
                server.name = form.getName();
                server.location = form.getLocation();
                MosaicServerManagerForm.this.serversModel.setElementAt( server, selectedIndex );
            }
        };
        dlg.setModal( true );
        dlg.setTitle( "Edit Mosaic Server" );
        dlg.show();
    }

    private void doRemove()
    {
        int selectedIndex = this.serversList.getSelectedIndex();
        if( selectedIndex >= 0 )
        {
            this.serversModel.remove( selectedIndex );
        }
    }

    private abstract class MosaicServerDialog extends DialogWrapper
    {
        @NotNull
        private final MosaicServerForm form = new MosaicServerForm();

        public MosaicServerDialog()
        {
            this( null );
        }

        public MosaicServerDialog( Server server )
        {
            super( MosaicServerManagerForm.this.serversList, true );
            init();
            initValidation();
            if( server != null )
            {
                this.form.setName( server.name );
                this.form.setLocation( server.location );
            }
        }

        @Override
        protected final void doOKAction()
        {
            doOKAction( this.form );
            super.doOKAction();
        }

        protected abstract void doOKAction( @NotNull MosaicServerForm form );

        @Nullable
        @Override
        protected ValidationInfo doValidate()
        {
            String name = this.form.getName();
            if( name.trim().isEmpty() )
            {
                return new ValidationInfo( "Server name has not been set" );
            }

            String location = this.form.getLocation();
            if( location.trim().isEmpty() )
            {
                return new ValidationInfo( "Server location has not been set" );
            }

            File dir = new File( location );
            if( !dir.exists() )
            {
                return new ValidationInfo( "Server location does not exist" );
            }
            else if( !dir.isDirectory() )
            {
                return new ValidationInfo( "Server location is not a directory" );
            }

            String version = this.form.getVersion();
            if( version.trim().isEmpty() || "Unknown".equalsIgnoreCase( version ) )
            {
                return new ValidationInfo( "Unrecognized server version" );
            }

            return super.doValidate();
        }

        @Nullable
        @Override
        protected JComponent createCenterPanel()
        {
            return this.form.getContainer();
        }
    }
}
